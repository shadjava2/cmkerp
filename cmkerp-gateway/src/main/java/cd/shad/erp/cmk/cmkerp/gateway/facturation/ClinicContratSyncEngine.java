package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;

/**
 * Moteur de rapprochement TCONTRAT : snapshot bidirectionnel, inbound automatique,
 * application manuelle et configuration effective.
 */
@Service
public class ClinicContratSyncEngine {

  private static final Logger log = LoggerFactory.getLogger(ClinicContratSyncEngine.class);

  private final ErpContratRepository repository;
  private final ClinicContratLegacyReader legacyReader;
  private final ClinicLegacyWriteGuard writeGuard;
  private final ObjectMapper objectMapper;

  private volatile Instant lastInboundAt;
  private volatile String lastInboundResult = "Jamais exécuté";
  private volatile int lastInboundUpdated;
  private volatile int lastInboundConflicts;
  private volatile int lastInboundNew;

  public ClinicContratSyncEngine(
      ErpContratRepository repository,
      ClinicContratLegacyReader legacyReader,
      ClinicLegacyWriteGuard writeGuard,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.legacyReader = legacyReader;
    this.writeGuard = writeGuard;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> config() {
    var beach = writeGuard.properties().getBeach();
    Map<String, Object> desired = new LinkedHashMap<>();
    desired.put("mode", beach.isWriteEnabled() && beach.isInboundEnabled()
        ? "BIDIRECTIONAL"
        : beach.isInboundEnabled()
            ? "CLINIQUE_TO_ERP"
            : beach.isWriteEnabled() ? "ERP_TO_CLINIQUE" : "READ_ONLY");
    desired.put("autoInbound", beach.isInboundEnabled());
    desired.put("autoOutbound", beach.isSyncEnabled() && beach.isWriteEnabled());
    desired.put("inboundIntervalMs", beach.getInboundIntervalMs());
    desired.put("batchSize", 20);
    desired.put("maxAttempts", beach.getMaxAttempts());

    Map<String, Object> server = new LinkedHashMap<>();
    server.put("syncEnabled", beach.isSyncEnabled());
    server.put("writeEnabled", beach.isWriteEnabled());
    server.put("writeEntities", List.copyOf(beach.writeEntitySet()));
    server.put("inboundEnabled", beach.isInboundEnabled());
    server.put("deleteEnabled", beach.isDeleteEnabled());
    server.put("chWriteEnabled", writeGuard.chWriteEnabled());
    server.put("excludeCodes", List.copyOf(beach.excludeCodeSet()));
    server.put("capabilities", writeGuard.properties().effectiveCapabilities());

    Map<String, Object> effective = new LinkedHashMap<>();
    effective.put("tcontratOutbound", writeGuard.beachEntityWriteEnabled("TCONTRAT"));
    effective.put("tcontratInbound", beach.isSyncEnabled() && beach.isInboundEnabled());
    effective.put("workerActive", beach.isSyncEnabled());
    effective.put("lastInboundAt", lastInboundAt == null ? null : lastInboundAt.toString());
    effective.put("lastInboundResult", lastInboundResult);
    effective.put("lastInboundUpdated", lastInboundUpdated);
    effective.put("lastInboundConflicts", lastInboundConflicts);
    effective.put("lastInboundNew", lastInboundNew);

    Map<String, Object> result = new LinkedHashMap<>();
    result.put("entity", "TCONTRAT");
    result.put("desired", desired);
    result.put("server", server);
    result.put("effective", effective);
    result.put("note", "La configuration UI ne peut pas outrepasser les protections serveur.");
    return result;
  }

  public Map<String, Object> snapshot() {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("entity", "TCONTRAT");
    result.put("comparedAt", Instant.now().toString());
    result.put("beachAvailable", legacyReader.available());
    result.put("outboundWriteAllowed", writeGuard.beachEntityWriteEnabled("TCONTRAT"));
    result.put("inboundApplyAllowed", true);

    List<Map<String, Object>> outbound = new ArrayList<>();
    List<Map<String, Object>> inbound = new ArrayList<>();
    List<Map<String, Object>> divergences = new ArrayList<>();

    if (!legacyReader.available()) {
      result.put("outbound", outbound);
      result.put("inbound", inbound);
      result.put("divergences", divergences);
      result.put("kpis", Map.of("outbound", 0, "inbound", 0, "divergences", 0));
      result.put("error", "Source CLINIQUE indisponible");
      return result;
    }

    Map<String, ClinicContratLegacyRow> clinic = new HashMap<>();
    for (ClinicContratLegacyRow row : legacyReader.listAll()) {
      String code = safeCode(row.code());
      if (code != null) {
        clinic.put(code, row);
      }
    }

    Set<String> erpCodes = new HashSet<>();
    for (ErpContratRecord erp : repository.search(null, null, 500, 0)) {
      erpCodes.add(erp.code());
      ClinicContratLegacyRow c = clinic.get(erp.code());
      String erpFp = ClinicContratFingerprint.of(erp.code(), erp.designation());
      if (c == null) {
        outbound.add(row(
            erp.code(),
            erp.designation(),
            erp.designation(),
            null,
            erp.syncStatus(),
            classifyOutbound(erp)));
        continue;
      }
      String clinicFp = ClinicContratFingerprint.of(c.code(), c.designation());
      if (!erpFp.equals(clinicFp)) {
        if ("OPERATION_ERP".equalsIgnoreCase(erp.sourceSystem())
            || "PENDING".equalsIgnoreCase(erp.syncStatus())
            || "SUSPENDED".equalsIgnoreCase(erp.syncStatus())
            || "FAILED".equalsIgnoreCase(erp.syncStatus())) {
          outbound.add(row(
              erp.code(), erp.designation(), erp.designation(), c.designation(), erp.syncStatus(), "A_RAPPROCHER"));
        }
        if ("IMPORT_CLINIQUE".equalsIgnoreCase(erp.sourceSystem())
            || "SYNCED".equalsIgnoreCase(erp.syncStatus())
            || "IMPORTED".equalsIgnoreCase(erp.syncStatus())) {
          inbound.add(row(
              erp.code(), c.designation(), erp.designation(), c.designation(), "CHANGED", "MODIFICATION_CLINIQUE"));
        }
        divergences.add(row(
            erp.code(), erp.designation(), erp.designation(), c.designation(), "DIVERGENCE", "DIVERGENCE"));
      } else if (needsOutboundIntent(erp)) {
        outbound.add(row(
            erp.code(), erp.designation(), erp.designation(), c.designation(), erp.syncStatus(), "INTENTION_MANQUANTE"));
      }
    }

    for (Map.Entry<String, ClinicContratLegacyRow> e : clinic.entrySet()) {
      if (!erpCodes.contains(e.getKey())) {
        ClinicContratLegacyRow c = e.getValue();
        inbound.add(row(c.code(), c.designation(), null, c.designation(), "NEW", "NOUVEAU_CLINIQUE"));
      }
    }

    result.put("outbound", outbound);
    result.put("inbound", inbound);
    result.put("divergences", divergences);
    result.put("kpis", Map.of(
        "outbound", outbound.size(),
        "inbound", inbound.size(),
        "divergences", divergences.size()));
    return result;
  }

  /**
   * Rapprochement entrant automatique : applique les modifications CLINIQUE sur les fiches
   * ERP d'origine IMPORT_CLINIQUE / SYNCED sans boucler vers l'outbox. Les conflits ERP restent bloqués.
   */
  @Transactional
  public Map<String, Object> runInboundReconcile(boolean autoApply) {
    Map<String, Object> report = new LinkedHashMap<>();
    report.put("entity", "TCONTRAT");
    report.put("autoApply", autoApply);
    report.put("at", Instant.now().toString());
    if (!legacyReader.available()) {
      report.put("ok", false);
      report.put("error", "Source CLINIQUE indisponible");
      lastInboundResult = "Source indisponible";
      lastInboundAt = Instant.now();
      return report;
    }

    int updated = 0;
    int conflicts = 0;
    int discoveredNew = 0;
    int unchanged = 0;
    List<String> newCodes = new ArrayList<>();

    Map<String, ClinicContratLegacyRow> clinic = new HashMap<>();
    for (ClinicContratLegacyRow row : legacyReader.listAll()) {
      String code = safeCode(row.code());
      if (code != null) {
        clinic.put(code, row);
      }
    }

    Set<String> erpCodes = new HashSet<>();
    for (ErpContratRecord erp : repository.search(null, null, 500, 0)) {
      erpCodes.add(erp.code());
      ClinicContratLegacyRow c = clinic.get(erp.code());
      if (c == null) {
        continue;
      }
      String clinicFp = ClinicContratFingerprint.of(c.code(), c.designation());
      String erpFp = ClinicContratFingerprint.of(erp.code(), erp.designation());
      if (clinicFp.equals(erpFp)) {
        unchanged++;
        repository.updateFingerprint(erp.id(), clinicFp);
        continue;
      }
      boolean erpOwned = "OPERATION_ERP".equalsIgnoreCase(erp.sourceSystem())
          && repository.hasOpenOutbox(erp.id(), "TCONTRAT");
      if (erpOwned || "PENDING".equalsIgnoreCase(erp.syncStatus()) || "SUSPENDED".equalsIgnoreCase(erp.syncStatus())) {
        conflicts++;
        repository.updateSyncStatus(erp.id(), "CONFLICT");
        continue;
      }
      if (autoApply) {
        // Pas d'enqueue outbound — évite les boucles.
        repository.updateDesignationFromInbound(erp.id(), c.designation(), clinicFp, "SYNCED");
        updated++;
      }
    }

    for (String code : clinic.keySet()) {
      if (!erpCodes.contains(code)) {
        discoveredNew++;
        newCodes.add(code);
      }
    }

    lastInboundAt = Instant.now();
    lastInboundUpdated = updated;
    lastInboundConflicts = conflicts;
    lastInboundNew = discoveredNew;
    lastInboundResult = "MAJ=" + updated + " conflits=" + conflicts + " nouveaux=" + discoveredNew;

    report.put("ok", true);
    report.put("updated", updated);
    report.put("conflicts", conflicts);
    report.put("unchanged", unchanged);
    report.put("discoveredNew", discoveredNew);
    report.put("newCodesSample", newCodes.stream().limit(20).toList());
    log.info("Inbound TCONTRAT : {}", lastInboundResult);
    return report;
  }

  @Transactional
  public Map<String, Object> applyInbound(List<String> codes) {
    if (codes == null || codes.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucune sélection");
    }
    if (!legacyReader.available()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Source CLINIQUE indisponible");
    }
    Map<String, ClinicContratLegacyRow> clinic = new HashMap<>();
    for (ClinicContratLegacyRow row : legacyReader.listAll()) {
      String code = safeCode(row.code());
      if (code != null) {
        clinic.put(code, row);
      }
    }
    int imported = 0;
    int updated = 0;
    int skipped = 0;
    int failed = 0;
    for (String raw : codes) {
      String code = safeCode(raw);
      if (code == null) {
        skipped++;
        continue;
      }
      ClinicContratLegacyRow c = clinic.get(code);
      if (c == null) {
        failed++;
        continue;
      }
      try {
        var existing = repository.findByCode(code);
        String fp = ClinicContratFingerprint.of(c.code(), c.designation());
        if (existing.isEmpty()) {
          long id = repository.insert(code, c.designation(), true, "IMPORT_CLINIQUE", code, "IMPORTED", null);
          repository.updateFingerprint(id, fp);
          imported++;
        } else {
          ErpContratRecord erp = existing.get();
          if (repository.hasOpenOutbox(erp.id(), "TCONTRAT")
              && "OPERATION_ERP".equalsIgnoreCase(erp.sourceSystem())) {
            repository.updateSyncStatus(erp.id(), "CONFLICT");
            skipped++;
            continue;
          }
          repository.updateDesignationFromInbound(erp.id(), c.designation(), fp, "SYNCED");
          updated++;
        }
      } catch (Exception ex) {
        failed++;
      }
    }
    return Map.of("applied", imported + updated, "imported", imported, "updated", updated, "skipped", skipped, "failed", failed);
  }

  @Transactional
  public Map<String, Object> applyOutbound(List<String> codes, Long userId) {
    if (!writeGuard.beachEntityWriteEnabled("TCONTRAT")) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Écritures Beach TCONTRAT désactivées — intentions journalisées en SUSPENDED uniquement si préparation.");
    }
    // Remet en PENDING (y compris intentions déjà SUSPENDED) ; le worker pousse vers CLINIQUE.
    int prepared = 0;
    int skipped = 0;
    Set<String> exclude = writeGuard.properties().getBeach().excludeCodeSet();
    for (String raw : codes == null ? List.<String>of() : codes) {
      String code = safeCode(raw);
      if (code == null || exclude.contains(code.toUpperCase(Locale.ROOT))) {
        skipped++;
        continue;
      }
      var erpOpt = repository.findByCode(code);
      if (erpOpt.isEmpty()) {
        skipped++;
        continue;
      }
      ErpContratRecord erp = erpOpt.get();
      if (repository.hasOpenOutbox(erp.id(), "TCONTRAT")) {
        int requeued = repository.requeueOpenOutboxToPending(erp.id(), "TCONTRAT");
        if (requeued <= 0) {
          skipped++;
          continue;
        }
        repository.updateSyncStatus(erp.id(), "PENDING");
        prepared++;
        continue;
      }
      enqueue(erp.id(), erp.code(), erp.designation(), erp.legacyCode() == null ? "INSERT" : "UPDATE", userId, "PENDING");
      repository.updateSyncStatus(erp.id(), "PENDING");
      prepared++;
    }
    return Map.of("applied", prepared, "skipped", skipped, "failed", 0);
  }

  @Transactional
  public Map<String, Object> resolveDivergences(List<Map<String, Object>> items, Long userId) {
    int applied = 0;
    int skipped = 0;
    int failed = 0;
    for (Map<String, Object> item : items == null ? List.<Map<String, Object>>of() : items) {
      String code = safeCode(String.valueOf(item.get("code") == null ? item.get("id") : item.get("code")));
      String resolution = String.valueOf(item.get("resolution") == null ? "" : item.get("resolution")).toUpperCase(Locale.ROOT);
      if (code == null || resolution.isBlank()) {
        skipped++;
        continue;
      }
      try {
        if ("KEEP_CLINIQUE".equals(resolution) || "CLINIQUE".equals(resolution)) {
          Map<String, Object> r = applyInbound(List.of(code));
          applied += ((Number) r.getOrDefault("applied", 0)).intValue();
        } else if ("KEEP_ERP".equals(resolution) || "ERP".equals(resolution)) {
          var erpOpt = repository.findByCode(code);
          if (erpOpt.isEmpty()) {
            skipped++;
            continue;
          }
          ErpContratRecord erp = erpOpt.get();
          if (isExcluded(code)) {
            skipped++;
            continue;
          }
          String status = writeGuard.beachEntityWriteEnabled("TCONTRAT") ? "PENDING" : "SUSPENDED";
          if (!repository.hasOpenOutbox(erp.id(), "TCONTRAT")) {
            enqueue(erp.id(), erp.code(), erp.designation(), "UPDATE", userId, status);
          }
          repository.updateSyncStatus(erp.id(), status);
          applied++;
        } else {
          skipped++;
        }
      } catch (Exception ex) {
        failed++;
      }
    }
    return Map.of("applied", applied, "skipped", skipped, "failed", failed);
  }

  @Transactional
  public Map<String, Object> prepareMissingIntentions(Long userId, boolean includeExcluded) {
    int prepared = 0;
    int skipped = 0;
    int excluded = 0;
    for (ErpContratRecord row : repository.listBySourceSystem("OPERATION_ERP")) {
      if (!includeExcluded && isExcluded(row.code())) {
        excluded++;
        continue;
      }
      if (repository.hasOpenOutbox(row.id(), "TCONTRAT")) {
        skipped++;
        continue;
      }
      String status = writeGuard.beachEntityWriteEnabled("TCONTRAT") ? "PENDING" : "SUSPENDED";
      enqueue(row.id(), row.code(), row.designation(), "INSERT", userId, status);
      repository.updateSyncStatus(row.id(), status);
      prepared++;
    }
    return Map.of("prepared", prepared, "skipped", skipped, "excluded", excluded);
  }

  public Map<String, Object> history(String status, String entityType, String q, int page, int size) {
    int safeSize = Math.min(Math.max(size, 1), 100);
    int safePage = Math.max(page, 0);
    List<Map<String, Object>> items =
        repository.listOutboxHistory(status, entityType, q, safeSize, safePage * safeSize);
    return Map.of(
        "items", items,
        "total", repository.countOutboxHistory(status, entityType, q),
        "page", safePage,
        "size", safeSize);
  }

  private void enqueue(long id, String code, String designation, String op, Long userId, String status) {
    writeGuard.assertChWriteForbidden();
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("code", code);
      payload.put("designation", designation == null ? "" : designation);
      payload.put("operation", op);
      payload.put("userId", userId == null ? 0 : userId);
      payload.put("columns", List.of("CODE", "DESIGNATION"));
      payload.put("fingerprint", ClinicContratFingerprint.of(code, designation));
      repository.enqueueOutbox(
          UUID.randomUUID().toString(),
          "TCONTRAT",
          id,
          code,
          op,
          objectMapper.writeValueAsString(payload),
          UUID.randomUUID().toString(),
          status);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Journalisation impossible");
    }
  }

  private boolean isExcluded(String code) {
    if (code == null) {
      return false;
    }
    return writeGuard.properties().getBeach().excludeCodeSet().contains(code.trim().toUpperCase(Locale.ROOT));
  }

  private static boolean needsOutboundIntent(ErpContratRecord erp) {
    if (!"OPERATION_ERP".equalsIgnoreCase(erp.sourceSystem())) {
      return false;
    }
    String s = erp.syncStatus() == null ? "" : erp.syncStatus().toUpperCase(Locale.ROOT);
    return "LOCAL_ONLY".equals(s) || "SUSPENDED".equals(s) || "PENDING".equals(s) || "FAILED".equals(s);
  }

  private static String classifyOutbound(ErpContratRecord erp) {
    String s = erp.syncStatus() == null ? "" : erp.syncStatus().toUpperCase(Locale.ROOT);
    if ("FAILED".equals(s)) {
      return "ECHEC";
    }
    if ("SUSPENDED".equals(s)) {
      return "SUSPENDU";
    }
    if ("PENDING".equals(s) || "IN_PROGRESS".equals(s)) {
      return "EN_ATTENTE";
    }
    return "ABSENT_CLINIQUE";
  }

  private static Map<String, Object> row(
      String code, String designation, String erpDesignation, String clinicDesignation, String status, String state) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("id", code);
    m.put("code", code);
    m.put("designation", designation);
    m.put("erpDesignation", erpDesignation);
    m.put("clinicDesignation", clinicDesignation);
    m.put("status", status);
    m.put("state", state);
    return m;
  }

  private static String safeCode(String code) {
    if (code == null || code.isBlank()) {
      return null;
    }
    try {
      return ErpContratRepository.normalizeCode(code);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }
}
