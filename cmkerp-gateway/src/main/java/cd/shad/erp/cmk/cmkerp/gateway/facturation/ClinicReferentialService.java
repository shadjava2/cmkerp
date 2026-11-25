package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Mode;
import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialRepository.Snap;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.SiteResponse;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;
import cd.shad.erp.cmk.cmkerp.platform.site.application.service.SiteQueryService;

@Service
public class ClinicReferentialService {

  private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
  };

  /** Champs ERP locaux — jamais répliqués vers SQL Server CLINIQUE. */
  private static final Set<String> ERP_OVERLAY_KEYS = Set.of(
      "SITEID", "SITE_ID", "MEDILINEIPP", "MEDILINE_IPP", "MEDILINEPATIENTID", "MEDILINE_PATIENT_ID",
      "PATIENTLINKSTATUS", "PATIENT_LINK_STATUS", "CONTRATID", "CONTRAT_ID", "SOCIETEID", "SOCIETE_ID");

  private final ClinicReferentialRepository repository;
  private final ClinicReferentialLegacyReader legacyReader;
  private final ClinicLegacyWriteGuard writeGuard;
  private final SiteQueryService siteQueryService;
  private final ErpFacturationRelationalStore relationalStore;
  private final ObjectMapper objectMapper;

  public ClinicReferentialService(
      ClinicReferentialRepository repository,
      ClinicReferentialLegacyReader legacyReader,
      ClinicLegacyWriteGuard writeGuard,
      SiteQueryService siteQueryService,
      ErpFacturationRelationalStore relationalStore,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.legacyReader = legacyReader;
    this.writeGuard = writeGuard;
    this.siteQueryService = siteQueryService;
    this.relationalStore = relationalStore;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> list(String key, String q, Boolean actif, int page, int size) {
    Definition def = require(key);
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    // Lecture prioritaire des tables métier normalisées (libellés FK) lorsque disponibles.
    if (relationalStore.supports(def.code())) {
      List<Map<String, Object>> items =
          relationalStore.search(def.code(), q, actif, safeSize, safePage * safeSize);
      Map<String, Object> result = new LinkedHashMap<>();
      result.put("code", def.code());
      result.put("label", def.label());
      result.put("items", items);
      result.put("total", relationalStore.count(def.code(), q, actif));
      result.put("page", safePage);
      result.put("size", safeSize);
      result.put("relational", true);
      return result;
    }
    List<Map<String, Object>> items = repository
        .search(def.code(), q, actif, safeSize, safePage * safeSize)
        .stream()
        .map(this::toView)
        .toList();
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("code", def.code());
    result.put("label", def.label());
    result.put("items", items);
    result.put("total", repository.count(def.code(), q, actif));
    result.put("page", safePage);
    result.put("size", safeSize);
    result.put("relational", false);
    return result;
  }

  public Map<String, Object> get(String key, long id) {
    Definition def = require(key);
    if (relationalStore.supports(def.code())) {
      return relationalStore.findById(def.code(), id)
          .orElseGet(() -> toView(requireRow(def, id)));
    }
    return toView(requireRow(def, id));
  }

  @Transactional
  public Map<String, Object> create(String key, String naturalKey, Map<String, Object> fields, Long userId) {
    Definition def = require(key);
    assertMutable(def);
    Map<String, Object> overlays = extractOverlays(fields);
    Map<String, Object> normalized = ClinicReferentialValues.normalize(
        def, bindKey(def, naturalKey, stripOverlays(fields)), true, true);
    String nk = ClinicReferentialValues.naturalKey(def, normalized);
    if (repository.findByNaturalKey(def.code(), nk).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet élément existe déjà.");
    }
    Map<String, Object> payload = mergeOverlays(normalized, overlays);
    boolean replicate = def.outboundInsert();
    String syncStatus = !replicate
        ? "LOCAL_ONLY"
        : (writeGuard.beachEntityWriteEnabled(def.entityType()) ? "PENDING" : "SUSPENDED");
    String fingerprint = ClinicReferentialValues.fingerprint(def, normalized);
    long id = repository.insert(
        def.code(),
        nk,
        displayLabel(def, normalized),
        writeJson(payload),
        fingerprint,
        true,
        "OPERATION_ERP",
        null,
        syncStatus,
        userId);
    Map<String, Object> forMirror = new LinkedHashMap<>(normalized);
    forMirror.putAll(overlays);
    mirrorRelationalImport(def, nk, forMirror, fingerprint);
    if (replicate) {
      enqueue(def, id, nk, normalized, "INSERT");
    }
    if (relationalStore.supports(def.code())) {
      return relationalStore.search(def.code(), nk, null, 5, 0).stream()
          .filter(row -> nk.equals(String.valueOf(row.get("key"))))
          .findFirst()
          .orElseGet(() -> toView(requireRow(def, id)));
    }
    return toView(requireRow(def, id));
  }

  @Transactional
  public Map<String, Object> update(String key, long id, Map<String, Object> fields, int version, Long userId) {
    Definition def = require(key);
    assertMutable(def);
    // Source de vérité métier = tables erp_* lorsque disponibles (IDs relationnels).
    if (relationalStore.supports(def.code())) {
      Map<String, Object> current = relationalStore.findById(def.code(), id)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élément introuvable."));
      String naturalKey = String.valueOf(current.get("key"));
      Map<String, Object> merged = new LinkedHashMap<>();
      Object existingFields = current.get("fields");
      if (existingFields instanceof Map<?, ?> map) {
        for (Map.Entry<?, ?> e : map.entrySet()) {
          if (e.getKey() != null) {
            merged.put(String.valueOf(e.getKey()), e.getValue());
          }
        }
      }
      if (fields != null) {
        merged.putAll(fields);
      }
      Map<String, Object> overlays = extractOverlays(merged);
      Map<String, Object> normalized = ClinicReferentialValues.normalize(
          def, bindKey(def, naturalKey, stripOverlays(merged)), true, true);
      String fingerprint = ClinicReferentialValues.fingerprint(def, normalized);
      if ("AYANTS_DROIT".equals(def.code())) {
        int updated = relationalStore.updateAyantById(
            id,
            first(normalized, "NOM"),
            str(normalized, "SEXE"),
            localDate(normalized.get("DATENAIS")),
            str(normalized, "RELATION"),
            str(normalized, "CODEFAM"),
            str(normalized, "CODESOC"),
            str(normalized, "CONTRAT"),
            first(overlays, "medilineIpp", "MEDILINE_IPP", "mediline_ipp"),
            longOrNull(firstRaw(overlays, "medilinePatientId", "MEDILINE_PATIENT_ID")),
            first(overlays, "patientLinkStatus", "PATIENT_LINK_STATUS"),
            version,
            userId);
        if (updated == 0) {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrence détectée — recharger la fiche.");
        }
      } else if ("SOCIETES".equals(def.code())) {
        Long contratId = relationalStore.findContratId(str(normalized, "CONTRAT"));
        int updated = relationalStore.updateSociete(
            id,
            str(normalized, "NOMSOC"),
            str(normalized, "ADRESSE"),
            str(normalized, "N_IMPOT"),
            str(normalized, "NRC"),
            str(normalized, "ID_NAT"),
            contratId,
            str(normalized, "CONTRAT"),
            version,
            userId);
        if (updated == 0) {
          throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrence détectée — recharger la fiche.");
        }
      } else {
        mirrorRelationalImport(def, naturalKey, normalized, fingerprint);
        if ("AYANTS_DROIT".equals(def.code())) {
          // already handled
        }
      }
      // Pont d'import : synchroniser le bag JSON si la ligne existe encore.
      repository.findByNaturalKey(def.code(), naturalKey).ifPresent(row -> {
        Map<String, Object> payload = mergeOverlays(normalized, overlays);
        repository.updatePayload(
            def.code(),
            row.id(),
            displayLabel(def, normalized),
            writeJson(payload),
            fingerprint,
            "LOCAL_ONLY",
            row.version(),
            userId);
      });
      return relationalStore.findById(def.code(), id)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élément introuvable."));
    }

    ClinicReferentialRecord current = requireRow(def, id);
    Map<String, Object> merged = parsePayload(current.payloadJson());
    if (fields != null) {
      for (Map.Entry<String, Object> entry : fields.entrySet()) {
        if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(def.keyColumn())) {
          Object coerced = ClinicReferentialValues.coerce(def.keyColumnDef(), entry.getValue());
          String requested = coerced == null ? "" : String.valueOf(coerced).trim();
          if (!requested.equals(current.naturalKey())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'identifiant n'est pas modifiable.");
          }
        }
        merged.put(entry.getKey(), entry.getValue());
      }
    }
    merged.put(def.keyColumn(), current.naturalKey());
    Map<String, Object> overlays = extractOverlays(merged);
    Map<String, Object> normalized = ClinicReferentialValues.normalize(def, stripOverlays(merged), true, true);
    Map<String, Object> payload = mergeOverlays(normalized, overlays);
    String fingerprint = ClinicReferentialValues.fingerprint(def, normalized);
    String operation = resolveOperation(def, current);
    String syncStatus = operation == null
        ? "LOCAL_ONLY"
        : (writeGuard.beachEntityWriteEnabled(def.entityType()) ? "PENDING" : "SUSPENDED");
    int updated = repository.updatePayload(
        def.code(),
        id,
        displayLabel(def, normalized),
        writeJson(payload),
        fingerprint,
        syncStatus,
        version,
        userId);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrence détectée — recharger la fiche.");
    }
    if (operation != null) {
      enqueue(def, id, current.naturalKey(), normalized, operation);
    }
    return toView(requireRow(def, id));
  }

  public List<Map<String, Object>> listErpSites(String key) {
    Definition def = require(key);
    if (!"locations".equals(def.key())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Association site disponible uniquement pour les locations.");
    }
    List<Map<String, Object>> out = new ArrayList<>();
    for (SiteResponse site : siteQueryService.findAll()) {
      Map<String, Object> row = new LinkedHashMap<>();
      row.put("id", site.getId());
      row.put("designation", site.getDesignation());
      row.put("abbreviation", site.getAbbreviation());
      row.put("adresse", site.getAdresse());
      out.add(row);
    }
    return out;
  }

  @Transactional
  public Map<String, Object> setActif(String key, long id, boolean actif, int version, Long userId) {
    Definition def = require(key);
    if (relationalStore.supports(def.code())) {
      int updated = relationalStore.updateActif(def.code(), id, actif, version, userId);
      if (updated == 0) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrence détectée — recharger la fiche.");
      }
      return relationalStore.findById(def.code(), id)
          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élément introuvable."));
    }
    int updated = repository.updateActif(def.code(), id, actif, version, userId);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrence détectée — recharger la fiche.");
    }
    return toView(requireRow(def, id));
  }

  public Map<String, Object> listImportCandidates(String key, String q, int page, int size) {
    Definition def = require(key);
    if (!legacyReader.available()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "La source historique est temporairement indisponible.");
    }
    int safeSize = Math.min(Math.max(size, 1), ClinicReferentialCatalog.IMPORT_CANDIDATES_PAGE_MAX);
    int safePage = Math.max(page, 0);
    int sourceTotal = legacyReader.count(def, q);
    boolean complete = sourceTotal <= ClinicReferentialCatalog.FULL_COMPARE_LIMIT;
    boolean windowed = !complete
        || sourceTotal > ClinicReferentialCatalog.SUPERVISION_DETAILED_COMPARE_LIMIT;
    Map<String, Object> report = baseReport(def);
    report.put("sourceTotal", sourceTotal);
    report.put("comparisonComplete", complete && !windowed);
    report.put("windowed", windowed);
    report.put("page", safePage);
    report.put("size", safeSize);
    if (windowed) {
      List<Map<String, Object>> window = legacyReader.page(def, q, safePage * safeSize, safeSize);
      annotateWindow(def, window, report);
      long erpTotal = repository.count(def.code(), null, null);
      // Estimations globales (fenêtre Beach paginée — pas de scan complet à chaque page).
      report.put("alreadyImported", erpTotal);
      report.put("available", Math.max(0L, sourceTotal - erpTotal));
      report.put("unchanged", null);
      report.put("divergences", null);
      return report;
    }
    List<Map<String, Object>> rows = sourceTotal == 0
        ? List.of()
        : legacyReader.page(def, q, 0, sourceTotal);
    Map<String, Snap> erp = repository.index(def.code());
    List<Map<String, Object>> news = new ArrayList<>();
    List<Map<String, Object>> divergences = new ArrayList<>();
    int already = 0;
    int unchanged = 0;
    for (Map<String, Object> row : rows) {
      Map<String, Object> normalized = ClinicReferentialValues.normalize(def, row, false, false);
      String nk = ClinicReferentialValues.naturalKey(def, normalized);
      Snap snap = erp.get(nk);
      if (snap == null) {
        news.add(candidate(def, nk, normalized, "NEW", null));
        continue;
      }
      already++;
      String fingerprint = ClinicReferentialValues.fingerprint(def, normalized);
      if (fingerprint.equals(snap.fingerprint())) {
        unchanged++;
      } else {
        divergences.add(candidate(def, nk, normalized, "DIVERGENCE", snap));
      }
    }
    report.put("alreadyImported", already);
    report.put("available", news.size());
    report.put("unchanged", unchanged);
    report.put("divergences", divergences.size());
    report.put("candidates", pageOf(news, safePage, safeSize));
    report.put("divergenceItems", pageOf(divergences, safePage, safeSize));
    return report;
  }

  public Map<String, Object> dryRun(String key) {
    Map<String, Object> candidates = listImportCandidates(key, null, 0, 50);
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("code", candidates.get("code"));
    report.put("label", candidates.get("label"));
    report.put("dryRun", true);
    report.put("writesApplied", 0);
    report.put("comparisonComplete", candidates.get("comparisonComplete"));
    report.put("analyzed", candidates.get("sourceTotal"));
    report.put("newRows", candidates.get("available"));
    report.put("existingRows", candidates.get("alreadyImported"));
    report.put("unchanged", candidates.get("unchanged"));
    report.put("modified", candidates.get("divergences"));
    return report;
  }

  @Transactional
  public Map<String, Object> importSelected(String key, List<String> requested) {
    Definition def = require(key);
    if (!legacyReader.available()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "La source historique est temporairement indisponible.");
    }
    LinkedHashSet<String> wanted = normalizeSelection(def, requested);
    List<Map<String, Object>> clinicRows = legacyReader.findByKeys(def, List.copyOf(wanted));
    Map<String, Map<String, Object>> byKey = new LinkedHashMap<>();
    for (Map<String, Object> row : clinicRows) {
      Map<String, Object> normalized = ClinicReferentialValues.normalize(def, row, false, false);
      byKey.put(ClinicReferentialValues.naturalKey(def, normalized), normalized);
    }
    int imported = 0;
    int skipped = 0;
    int failed = 0;
    for (String nk : wanted) {
      Map<String, Object> normalized = byKey.get(nk);
      if (normalized == null) {
        failed++;
        continue;
      }
      String fingerprint = ClinicReferentialValues.fingerprint(def, normalized);
      int created = repository.insertIfAbsent(
          def.code(),
          nk,
          displayLabel(def, normalized),
          writeJson(normalized),
          fingerprint);
      mirrorRelationalImport(def, nk, normalized, fingerprint);
      if (created == 1) {
        imported++;
      } else {
        skipped++;
      }
    }
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("code", def.code());
    result.put("label", def.label());
    result.put("requested", wanted.size());
    result.put("imported", imported);
    result.put("skipped", skipped);
    result.put("failed", failed);
    return result;
  }

  public List<Map<String, Object>> supervisionSummaries() {
    boolean beach = legacyReader.available();
    boolean schemaMissing = false;
    List<Map<String, Object>> rows = new ArrayList<>();
    for (Definition def : ClinicReferentialCatalog.all()) {
      Map<String, Object> item = new LinkedHashMap<>();
      item.put("code", def.code());
      item.put("label", def.label());
      item.put("integrated", true);
      item.put("mode", modeLabel(def));
      Long erpCount = null;
      boolean erpFailed = schemaMissing;
      if (!schemaMissing) {
        try {
          erpCount = relationalStore.supports(def.code())
              ? relationalStore.count(def.code(), null, null)
              : repository.count(def.code(), null, null);
        } catch (DataAccessException ex) {
          schemaMissing = true;
          erpFailed = true;
        }
      }
      Integer clinicCount = null;
      boolean clinicFailed = !beach;
      if (beach) {
        try {
          clinicCount = legacyReader.count(def, null);
        } catch (DataAccessException ex) {
          clinicFailed = true;
        }
      }
      Map<String, Long> outbox = Map.of();
      try {
        outbox = repository.outboxCounts(def.entityType());
      } catch (DataAccessException ex) {
        outbox = Map.of();
      }
      long pending = outbox.getOrDefault("PENDING", 0L);
      long suspended = outbox.getOrDefault("SUSPENDED", 0L);
      long inProgress = outbox.getOrDefault("IN_PROGRESS", 0L);
      item.put("erpCount", erpCount);
      item.put("clinicCount", clinicCount);
      item.put("pending", pending);
      item.put("suspended", suspended);
      item.put("failed", outbox.getOrDefault("FAILED", 0L));
      item.put("inProgress", inProgress);
      enrichCompareCounters(def, item, erpCount, clinicCount, clinicFailed);
      item.put("state", supervisionState(erpFailed, clinicFailed, erpCount, clinicCount, pending, suspended, inProgress));
      rows.add(item);
    }
    return rows;
  }

  private void enrichCompareCounters(
      Definition def,
      Map<String, Object> item,
      Long erpCount,
      Integer clinicCount,
      boolean clinicFailed) {
    item.put("erpOnly", null);
    item.put("clinicOnly", null);
    item.put("matched", null);
    item.put("divergences", null);
    if (clinicFailed || erpCount == null || clinicCount == null) {
      return;
    }
    if (clinicCount > ClinicReferentialCatalog.SUPERVISION_DETAILED_COMPARE_LIMIT) {
      return;
    }
    try {
      List<Map<String, Object>> rows = clinicCount == 0
          ? List.of()
          : legacyReader.page(def, null, 0, clinicCount);
      Map<String, Snap> erp = repository.index(def.code());
      int clinicOnly = 0;
      int matched = 0;
      int divergences = 0;
      LinkedHashSet<String> seen = new LinkedHashSet<>();
      for (Map<String, Object> row : rows) {
        Map<String, Object> normalized = ClinicReferentialValues.normalize(def, row, false, false);
        String nk = ClinicReferentialValues.naturalKey(def, normalized);
        seen.add(nk);
        Snap snap = erp.get(nk);
        if (snap == null) {
          clinicOnly++;
          continue;
        }
        if (ClinicReferentialValues.fingerprint(def, normalized).equals(snap.fingerprint())) {
          matched++;
        } else {
          divergences++;
        }
      }
      int erpOnly = 0;
      for (String key : erp.keySet()) {
        if (!seen.contains(key)) {
          erpOnly++;
        }
      }
      item.put("erpOnly", erpOnly);
      item.put("clinicOnly", clinicOnly);
      item.put("matched", matched);
      item.put("divergences", divergences);
    } catch (DataAccessException | ResponseStatusException ex) {
      // Compteurs détaillés optionnels — ne pas faire échouer le centre.
    }
  }

  private void annotateWindow(Definition def, List<Map<String, Object>> window, Map<String, Object> report) {
    List<String> keys = new ArrayList<>();
    List<Map<String, Object>> normalizedRows = new ArrayList<>();
    for (Map<String, Object> row : window) {
      Map<String, Object> normalized = ClinicReferentialValues.normalize(def, row, false, false);
      normalizedRows.add(normalized);
      keys.add(ClinicReferentialValues.naturalKey(def, normalized));
    }
    Map<String, Snap> snaps = repository.findSnaps(def.code(), keys);
    List<Map<String, Object>> news = new ArrayList<>();
    List<Map<String, Object>> divergences = new ArrayList<>();
    for (int i = 0; i < normalizedRows.size(); i++) {
      Map<String, Object> normalized = normalizedRows.get(i);
      String nk = keys.get(i);
      Snap snap = snaps.get(nk);
      if (snap == null) {
        news.add(candidate(def, nk, normalized, "NEW", null));
      } else if (!ClinicReferentialValues.fingerprint(def, normalized).equals(snap.fingerprint())) {
        divergences.add(candidate(def, nk, normalized, "DIVERGENCE", snap));
      }
    }
    report.put("candidates", news);
    report.put("divergenceItems", divergences);
  }

  private Map<String, Object> candidate(
      Definition def, String naturalKey, Map<String, Object> fields, String state, Snap snap) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", naturalKey);
    item.put("key", naturalKey);
    item.put("code", naturalKey);
    item.put("label", displayLabel(def, fields));
    item.put("designation", displayLabel(def, fields));
    item.put("state", state);
    item.put("fields", fields);
    if (snap != null) {
      item.put("erpLabel", snap.label());
      item.put("erpActif", snap.actif());
    }
    return item;
  }

  private void enqueue(Definition def, long id, String naturalKey, Map<String, Object> fields, String operation) {
    writeGuard.assertChWriteForbidden();
    if (repository.hasBusyOutbox(id, def.entityType())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Une synchronisation est déjà en cours.");
    }
    String payload = writeJson(Map.of(
        "key", naturalKey,
        "fields", fields,
        "operation", operation));
    int replaced = repository.replaceOpenPayload(id, def.entityType(), operation, payload, naturalKey);
    if (replaced > 0) {
      return;
    }
    repository.enqueueOutbox(
        UUID.randomUUID().toString(),
        def.entityType(),
        id,
        naturalKey,
        operation,
        payload,
        UUID.randomUUID().toString(),
        writeGuard.beachEntityWriteEnabled(def.entityType()) ? "PENDING" : "SUSPENDED");
  }

  private String resolveOperation(Definition def, ClinicReferentialRecord current) {
    if (current.legacyKey() != null && !current.legacyKey().isBlank()) {
      return "UPDATE";
    }
    if (!def.outboundInsert()) {
      return null;
    }
    return "INSERT";
  }

  private LinkedHashSet<String> normalizeSelection(Definition def, List<String> requested) {
    if (requested == null || requested.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune ligne sélectionnée.");
    }
    if (requested.size() > ClinicReferentialCatalog.IMPORT_SELECTION_LIMIT) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélection trop importante.");
    }
    LinkedHashSet<String> wanted = new LinkedHashSet<>();
    for (String raw : requested) {
      if (raw == null || raw.isBlank()) {
        continue;
      }
      Map<String, Object> normalized = ClinicReferentialValues.normalize(
          def, Map.of(def.keyColumn(), raw.trim()), false, false);
      wanted.add(ClinicReferentialValues.naturalKey(def, normalized));
    }
    if (wanted.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune ligne sélectionnée.");
    }
    return wanted;
  }

  private Map<String, Object> toView(ClinicReferentialRecord row) {
    Map<String, Object> item = new LinkedHashMap<>();
    item.put("id", row.id());
    item.put("key", row.naturalKey());
    item.put("label", row.label());
    item.put("actif", row.actif());
    item.put("fields", parsePayload(row.payloadJson()));
    item.put("sourceSystem", row.sourceSystem());
    item.put("legacyKey", row.legacyKey());
    item.put("syncStatus", row.syncStatus());
    item.put("version", row.version());
    item.put("lastSyncedAt", row.lastSyncedAt());
    return item;
  }

  private Map<String, Object> parsePayload(String json) {
    if (json == null || json.isBlank()) {
      return new LinkedHashMap<>();
    }
    try {
      Map<String, Object> parsed = objectMapper.readValue(json, MAP_TYPE);
      return parsed == null ? new LinkedHashMap<>() : parsed;
    } catch (JsonProcessingException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Lecture de la fiche impossible.");
    }
  }

  private String writeJson(Object value) {
    try {
      return objectMapper.writeValueAsString(value);
    } catch (JsonProcessingException ex) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Enregistrement impossible.");
    }
  }

  private Definition require(String key) {
    return ClinicReferentialCatalog.byKey(key)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Référentiel introuvable."));
  }

  private ClinicReferentialRecord requireRow(Definition def, long id) {
    return repository.findById(def.code(), id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Élément introuvable."));
  }

  private static void assertMutable(Definition def) {
    if (def.mode() != Mode.BIDIRECTIONAL) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette fiche n'est pas modifiable.");
    }
  }

  private static Map<String, Object> bindKey(Definition def, String naturalKey, Map<String, Object> fields) {
    Map<String, Object> merged = new LinkedHashMap<>();
    if (fields != null) {
      merged.putAll(fields);
    }
    if (naturalKey != null && !naturalKey.isBlank()) {
      merged.put(def.keyColumn(), naturalKey.trim());
    }
    return merged;
  }

  private static boolean isOverlayKey(String name) {
    if (name == null || name.isBlank()) {
      return false;
    }
    return ERP_OVERLAY_KEYS.contains(name.trim().toUpperCase(Locale.ROOT).replace("-", "_"));
  }

  private Map<String, Object> extractOverlays(Map<String, Object> fields) {
    Map<String, Object> overlays = new LinkedHashMap<>();
    if (fields == null) {
      return overlays;
    }
    for (Map.Entry<String, Object> entry : fields.entrySet()) {
      if (!isOverlayKey(entry.getKey())) {
        continue;
      }
      String normalizedKey = entry.getKey().trim().toUpperCase(Locale.ROOT).replace("-", "_");
      Object raw = entry.getValue();
      if ("SITEID".equals(normalizedKey) || "SITE_ID".equals(normalizedKey)) {
        if (raw == null || (raw instanceof String text && text.isBlank())) {
          overlays.put("siteId", null);
          continue;
        }
        try {
          long siteId = raw instanceof Number number ? number.longValue() : Long.parseLong(String.valueOf(raw).trim());
          if (siteId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Site ERP invalide.");
          }
          siteQueryService.findById(siteId);
          overlays.put("siteId", siteId);
        } catch (NumberFormatException ex) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Site ERP invalide.");
        }
        continue;
      }
      if ("MEDILINEIPP".equals(normalizedKey) || "MEDILINE_IPP".equals(normalizedKey)) {
        overlays.put("medilineIpp", raw == null ? null : String.valueOf(raw).trim());
        continue;
      }
      if ("MEDILINEPATIENTID".equals(normalizedKey) || "MEDILINE_PATIENT_ID".equals(normalizedKey)) {
        overlays.put("medilinePatientId", longOrNull(raw));
        continue;
      }
      if ("PATIENTLINKSTATUS".equals(normalizedKey) || "PATIENT_LINK_STATUS".equals(normalizedKey)) {
        overlays.put("patientLinkStatus", raw == null ? null : String.valueOf(raw).trim());
        continue;
      }
      if ("CONTRATID".equals(normalizedKey) || "CONTRAT_ID".equals(normalizedKey)) {
        overlays.put("contratId", longOrNull(raw));
        continue;
      }
      if ("SOCIETEID".equals(normalizedKey) || "SOCIETE_ID".equals(normalizedKey)) {
        overlays.put("societeId", longOrNull(raw));
      }
    }
    return overlays;
  }

  private static Map<String, Object> stripOverlays(Map<String, Object> fields) {
    if (fields == null || fields.isEmpty()) {
      return Map.of();
    }
    Map<String, Object> cleaned = new LinkedHashMap<>();
    for (Map.Entry<String, Object> entry : fields.entrySet()) {
      if (!isOverlayKey(entry.getKey())) {
        cleaned.put(entry.getKey(), entry.getValue());
      }
    }
    return cleaned;
  }

  private static Map<String, Object> mergeOverlays(Map<String, Object> beachFields, Map<String, Object> overlays) {
    Map<String, Object> payload = new LinkedHashMap<>(beachFields);
    if (overlays != null && !overlays.isEmpty()) {
      if (overlays.containsKey("siteId") && overlays.get("siteId") == null) {
        payload.remove("siteId");
      } else {
        payload.putAll(overlays);
      }
    }
    return payload;
  }

  private static String displayLabel(Definition def, Map<String, Object> normalized) {
    String label = ClinicReferentialValues.label(def, normalized);
    if (label != null) {
      return label;
    }
    Object fallback = normalized.get("LIBELLE");
    if (fallback == null) {
      fallback = normalized.get("NOM");
    }
    if (fallback == null) {
      return null;
    }
    String text = String.valueOf(fallback).trim();
    return text.isEmpty() ? null : text;
  }

  private String modeLabel(Definition def) {
    if (def.mode() == Mode.PROJECTION) {
      return "LECTURE_SEULE";
    }
    if (def.mode() == Mode.IMPORT_ONLY || !writeGuard.beachEntityWriteEnabled(def.entityType())) {
      return "CLINIQUE_TO_ERP";
    }
    return "BIDIRECTIONAL_GATED";
  }

  /** Miroir idempotent vers les tables métier — sans outbox Beach. */
  private void mirrorRelationalImport(
      Definition def, String naturalKey, Map<String, Object> normalized, String fingerprint) {
    // Historique facturation / stock : tables typées même si supports() est false (pas de CRUD générique).
    switch (def.code()) {
      case "PRESTATIONS" -> mirrorPrestationHistory(naturalKey, normalized, fingerprint);
      case "MEDICAMENTS" -> mirrorMedicamentHistory(naturalKey, normalized, fingerprint);
      case "STOCK" -> mirrorStockHistory(naturalKey, normalized, fingerprint);
      default -> {
        if (!relationalStore.supports(def.code())) {
          return;
        }
        switch (def.code()) {
          case "SERVICES" -> relationalStore.upsertServiceFromImport(
              naturalKey, str(normalized, "LIBELLE"), fingerprint);
          case "IMPUTATIONS" -> relationalStore.upsertImputationFromImport(
              naturalKey, first(normalized, "DESIGNATION", "LIBELLE"), fingerprint);
          case "LOCATIONS" -> relationalStore.upsertLocationFromImport(
              naturalKey,
              first(normalized, "LIBELLE", "DESIGNATION"),
              str(normalized, "ADRESSE"),
              longOrNull(normalized.get("siteId")),
              fingerprint);
          case "MEDECINS" -> relationalStore.upsertMedecinFromImport(
              naturalKey, first(normalized, "NOM", "NOMMED", "DESIGNATION"), fingerprint);
          case "SOCIETES" -> relationalStore.upsertSocieteFromImport(
              naturalKey,
              str(normalized, "NOMSOC"),
              str(normalized, "ADRESSE"),
              str(normalized, "N_IMPOT"),
              str(normalized, "NRC"),
              str(normalized, "ID_NAT"),
              str(normalized, "CONTRAT"),
              fingerprint);
          case "CLASSES" -> relationalStore.upsertClasseFromImport(
              naturalKey, first(normalized, "LIBELLE", "DESIGNATION"), str(normalized, "SERVICE"), fingerprint);
          case "TARIFS" -> relationalStore.upsertTarifFromImport(
              naturalKey,
              str(normalized, "LIBPRES"),
              str(normalized, "CODESERV"),
              str(normalized, "CLASSE"),
              str(normalized, "IMPUTATION"),
              decimal(normalized, "MONTANT"),
              decimal(normalized, "MONTPRIVE"),
              fingerprint);
          case "AYANTS_DROIT" -> relationalStore.upsertAyantFromImport(
              naturalKey,
              str(normalized, "NOM"),
              str(normalized, "SEXE"),
              localDate(normalized.get("DATENAIS")),
              str(normalized, "NATIONALITE"),
              str(normalized, "RELATION"),
              str(normalized, "CODEFAM"),
              str(normalized, "CODESOC"),
              str(normalized, "CONTRAT"),
              str(normalized, "ASSURANCE"),
              str(normalized, "COVID"),
              str(normalized, "LOCATION"),
              str(normalized, "SOURCE"),
              fingerprint,
              first(normalized, "medilineIpp", "MEDILINE_IPP"),
              longOrNull(normalized.get("medilinePatientId") != null
                  ? normalized.get("medilinePatientId")
                  : normalized.get("MEDILINE_PATIENT_ID")));
          default -> {
            // no-op
          }
        }
      }
    }
  }

  private void mirrorPrestationHistory(String naturalKey, Map<String, Object> row, String fingerprint) {
    String matricule = str(row, "MATRICULE");
    String tarifCode = str(row, "CODEPRES");
    String medCode = str(row, "CODEMED");
    relationalStore.upsertPrestationHistory(
        naturalKey,
        naturalKey,
        relationalStore.findAyantIdByMatricule(matricule),
        matricule,
        relationalStore.findSocieteId(str(row, "CODESOC")),
        relationalStore.findContratId(str(row, "CONTRASOC")),
        relationalStore.findTarifIdByCode(tarifCode),
        tarifCode,
        relationalStore.findServiceId(str(row, "CODESERV")),
        relationalStore.findImputationId(str(row, "IMPUTATION")),
        relationalStore.findMedecinId(medCode),
        medCode,
        relationalStore.findLocationId(str(row, "LOCATION")),
        decimal(row, "MONTANT"),
        timestampOf(row.get("DATEPRES")),
        writeJson(row),
        fingerprint);
  }

  private void mirrorMedicamentHistory(String naturalKey, Map<String, Object> row, String fingerprint) {
    String matricule = str(row, "MATRICULE");
    String productCode = str(row, "CODEPROD");
    String pharmacyCode = str(row, "CODEPHARM");
    String medCode = str(row, "CODEMED");
    relationalStore.upsertMedicamentHistory(
        naturalKey,
        relationalStore.findAyantIdByMatricule(matricule),
        matricule,
        relationalStore.findSocieteId(str(row, "CODESOC")),
        relationalStore.findContratId(str(row, "CONTRASOC")),
        relationalStore.findProductIdByLegacyCode(productCode),
        productCode,
        relationalStore.findMedecinId(medCode),
        medCode,
        relationalStore.findPharmacyIdByLegacyCode(pharmacyCode),
        pharmacyCode,
        relationalStore.findServiceId(str(row, "CODESERV")),
        relationalStore.findLocationId(str(row, "LOCATION")),
        decimal(row, "QTE"),
        decimal(row, "MONTANT"),
        timestampOf(row.get("DATEMVT")),
        writeJson(row),
        fingerprint);
  }

  private void mirrorStockHistory(String naturalKey, Map<String, Object> row, String fingerprint) {
    Long productId = relationalStore.findProductIdByLegacyCode(naturalKey);
    for (int n = 1; n <= 20; n++) {
      BigDecimal qty = decimalNullable(row, "STOCK" + n);
      if (qty == null || qty.compareTo(BigDecimal.ZERO) == 0) {
        continue;
      }
      String compartment = "STOCK" + n;
      relationalStore.upsertStockCompartment(
          naturalKey,
          productId,
          compartment,
          qty,
          fingerprint + ":" + compartment);
    }
  }

  private static BigDecimal decimalNullable(Map<String, Object> map, String key) {
    Object v = map.get(key);
    if (v == null) {
      return null;
    }
    if (v instanceof BigDecimal bd) {
      return bd;
    }
    if (v instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    String text = String.valueOf(v).trim();
    if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
      return null;
    }
    try {
      return new BigDecimal(text);
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static Timestamp timestampOf(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof Timestamp ts) {
      return ts;
    }
    if (raw instanceof java.util.Date d) {
      return new Timestamp(d.getTime());
    }
    String text = String.valueOf(raw).trim().replace('T', ' ');
    if (text.length() == 10) {
      text = text + " 00:00:00";
    } else if (text.length() > 19) {
      text = text.substring(0, 19);
    }
    try {
      return Timestamp.valueOf(text);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static String str(Map<String, Object> map, String key) {
    Object v = map.get(key);
    if (v == null) {
      return null;
    }
    String text = String.valueOf(v).trim();
    return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
  }

  private static String first(Map<String, Object> map, String... keys) {
    for (String key : keys) {
      String v = str(map, key);
      if (v != null) {
        return v;
      }
    }
    return null;
  }

  private static Object firstRaw(Map<String, Object> map, String... keys) {
    if (map == null) {
      return null;
    }
    for (String key : keys) {
      if (map.containsKey(key) && map.get(key) != null) {
        return map.get(key);
      }
    }
    return null;
  }

  private static BigDecimal decimal(Map<String, Object> map, String key) {
    Object v = map.get(key);
    if (v == null) {
      return BigDecimal.ZERO;
    }
    if (v instanceof BigDecimal bd) {
      return bd;
    }
    if (v instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    try {
      return new BigDecimal(String.valueOf(v).trim());
    } catch (NumberFormatException ex) {
      return BigDecimal.ZERO;
    }
  }

  private static Long longOrNull(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof Number number) {
      return number.longValue();
    }
    try {
      return Long.parseLong(String.valueOf(raw).trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }

  private static LocalDate localDate(Object raw) {
    if (raw == null) {
      return null;
    }
    if (raw instanceof LocalDate ld) {
      return ld;
    }
    String text = String.valueOf(raw).trim();
    if (text.length() >= 10) {
      try {
        return LocalDate.parse(text.substring(0, 10));
      } catch (Exception ignored) {
        return null;
      }
    }
    return null;
  }

  private static String supervisionState(
      boolean erpFailed,
      boolean clinicFailed,
      Long erpCount,
      Integer clinicCount,
      long pending,
      long suspended,
      long inProgress) {
    if (erpFailed) {
      return "INDISPONIBLE";
    }
    if (clinicFailed) {
      return "SOURCE_INDISPONIBLE";
    }
    if (pending + suspended + inProgress > 0) {
      return "EN_ATTENTE";
    }
    long erp = erpCount == null ? 0 : erpCount;
    int clinic = clinicCount == null ? 0 : clinicCount;
    if (erp == 0 && clinic > 0) {
      return "A_IMPORTER";
    }
    if (erp != clinic) {
      return "A_RAPPROCHER";
    }
    return "A_JOUR";
  }

  private static Map<String, Object> baseReport(Definition def) {
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("code", def.code());
    report.put("label", def.label());
    report.put("entity", def.entityType());
    report.put("sourceLabel", "CLINIQUE Beach");
    report.put("targetLabel", "CMK ERP");
    return report;
  }

  private static List<Map<String, Object>> pageOf(List<Map<String, Object>> rows, int page, int size) {
    int from = Math.min(page * size, rows.size());
    int to = Math.min(from + size, rows.size());
    return rows.subList(from, to);
  }
}
