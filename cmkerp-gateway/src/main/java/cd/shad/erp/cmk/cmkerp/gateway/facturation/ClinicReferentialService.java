package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;

@Service
public class ClinicReferentialService {

  private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
  };

  private final ClinicReferentialRepository repository;
  private final ClinicReferentialLegacyReader legacyReader;
  private final ClinicLegacyWriteGuard writeGuard;
  private final ObjectMapper objectMapper;

  public ClinicReferentialService(
      ClinicReferentialRepository repository,
      ClinicReferentialLegacyReader legacyReader,
      ClinicLegacyWriteGuard writeGuard,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.legacyReader = legacyReader;
    this.writeGuard = writeGuard;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> list(String key, String q, Boolean actif, int page, int size) {
    Definition def = require(key);
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
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
    return result;
  }

  public Map<String, Object> get(String key, long id) {
    Definition def = require(key);
    return toView(requireRow(def, id));
  }

  @Transactional
  public Map<String, Object> create(String key, String naturalKey, Map<String, Object> fields, Long userId) {
    Definition def = require(key);
    assertMutable(def);
    Map<String, Object> normalized = ClinicReferentialValues.normalize(
        def, bindKey(def, naturalKey, fields), true, true);
    String nk = ClinicReferentialValues.naturalKey(def, normalized);
    if (repository.findByNaturalKey(def.code(), nk).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet élément existe déjà.");
    }
    boolean replicate = def.outboundInsert();
    String syncStatus = !replicate
        ? "LOCAL_ONLY"
        : (writeGuard.beachEntityWriteEnabled(def.entityType()) ? "PENDING" : "SUSPENDED");
    long id = repository.insert(
        def.code(),
        nk,
        displayLabel(def, normalized),
        writeJson(normalized),
        ClinicReferentialValues.fingerprint(def, normalized),
        true,
        "OPERATION_ERP",
        null,
        syncStatus,
        userId);
    if (replicate) {
      enqueue(def, id, nk, normalized, "INSERT");
    }
    return toView(requireRow(def, id));
  }

  @Transactional
  public Map<String, Object> update(String key, long id, Map<String, Object> fields, int version, Long userId) {
    Definition def = require(key);
    assertMutable(def);
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
    Map<String, Object> normalized = ClinicReferentialValues.normalize(def, merged, true, true);
    String fingerprint = ClinicReferentialValues.fingerprint(def, normalized);
    String operation = resolveOperation(def, current);
    String syncStatus = operation == null
        ? "LOCAL_ONLY"
        : (writeGuard.beachEntityWriteEnabled(def.entityType()) ? "PENDING" : "SUSPENDED");
    int updated = repository.updatePayload(
        def.code(),
        id,
        displayLabel(def, normalized),
        writeJson(normalized),
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

  @Transactional
  public Map<String, Object> setActif(String key, long id, boolean actif, int version, Long userId) {
    Definition def = require(key);
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
    int safeSize = Math.min(Math.max(size, 1), 100);
    int safePage = Math.max(page, 0);
    int sourceTotal = legacyReader.count(def, q);
    boolean complete = sourceTotal <= ClinicReferentialCatalog.FULL_COMPARE_LIMIT;
    Map<String, Object> report = baseReport(def);
    report.put("sourceTotal", sourceTotal);
    report.put("comparisonComplete", complete);
    report.put("page", safePage);
    report.put("size", safeSize);
    if (!complete) {
      List<Map<String, Object>> window = legacyReader.page(def, q, safePage * safeSize, safeSize);
      annotateWindow(def, window, report);
      report.put("alreadyImported", null);
      report.put("available", null);
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
      int created = repository.insertIfAbsent(
          def.code(),
          nk,
          displayLabel(def, normalized),
          writeJson(normalized),
          ClinicReferentialValues.fingerprint(def, normalized));
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
          erpCount = repository.count(def.code(), null, null);
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
      item.put("state", supervisionState(erpFailed, clinicFailed, erpCount, clinicCount, pending, suspended, inProgress));
      rows.add(item);
    }
    return rows;
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
