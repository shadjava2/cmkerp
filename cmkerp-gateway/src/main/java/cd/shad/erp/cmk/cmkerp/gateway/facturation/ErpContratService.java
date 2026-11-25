package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;

@Service
public class ErpContratService {

  private final ErpContratRepository repository;
  private final ClinicContratLegacyReader legacyReader;
  private final ClinicLegacyWriteGuard writeGuard;
  private final ObjectMapper objectMapper;

  public ErpContratService(
      ErpContratRepository repository,
      ClinicContratLegacyReader legacyReader,
      ClinicLegacyWriteGuard writeGuard,
      ObjectMapper objectMapper) {
    this.repository = repository;
    this.legacyReader = legacyReader;
    this.writeGuard = writeGuard;
    this.objectMapper = objectMapper;
  }

  public Map<String, Object> list(String q, Boolean actif, int page, int size) {
    int safeSize = Math.min(Math.max(size, 1), 200);
    int safePage = Math.max(page, 0);
    int offset = safePage * safeSize;
    List<ErpContratRecord> items = repository.search(q, actif, safeSize, offset);
    long total = repository.count(q, actif);
    return Map.of(
        "items", items,
        "total", total,
        "page", safePage,
        "size", safeSize);
  }

  public ErpContratRecord get(long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrat introuvable"));
  }

  @Transactional
  public ErpContratRecord create(String code, String designation, Long userId) {
    String normalized = ErpContratRepository.normalizeCode(code);
    if (normalized == null || normalized.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Code contrat obligatoire");
    }
    if (repository.findByCode(normalized).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Code contrat déjà existant: " + normalized);
    }
    String syncStatus = writeGuard.beachWriteEnabled() ? "PENDING" : "LOCAL_ONLY";
    long id = repository.insert(normalized, designation, true, "OPERATION_ERP", null, syncStatus, userId);
    if (writeGuard.beachWriteEnabled()) {
      enqueueReplication(id, normalized, designation, "INSERT", userId);
    }
    return get(id);
  }

  @Transactional
  public ErpContratRecord update(long id, String designation, int version, Long userId) {
    ErpContratRecord current = get(id);
    int updated = repository.updateDesignation(id, designation, version, userId);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrence détectée — recharger le contrat");
    }
    if (writeGuard.beachWriteEnabled() && current.legacyCode() != null) {
      enqueueReplication(id, current.code(), designation, "UPDATE", userId);
    } else if (writeGuard.beachWriteEnabled() && current.legacyCode() == null) {
      enqueueReplication(id, current.code(), designation, "INSERT", userId);
    }
    return get(id);
  }

  @Transactional
  public ErpContratRecord setActif(long id, boolean actif, int version, Long userId) {
    // actif = ERP only — jamais de réplication SQL Server
    int updated = repository.updateActif(id, actif, version, userId);
    if (updated == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Concurrence détectée — recharger le contrat");
    }
    return get(id);
  }

  public Map<String, Object> dryRunImport() {
    Map<String, Object> candidates = listImportCandidates(null);
    Map<String, Object> report = new HashMap<>();
    report.put("entity", "TCONTRAT");
    report.put("dryRun", true);
    report.put("analyzed", candidates.get("sourceTotal"));
    report.put("newRows", candidates.get("available"));
    report.put("existingRows", candidates.get("alreadyImported"));
    report.put("unchanged", candidates.get("unchanged"));
    report.put("modified", candidates.get("divergences"));
    report.put("writesApplied", 0);
    return report;
  }

  /**
   * Rapprochement CLINIQUE Beach (SELECT) ↔ MySQL ERP. Aucune écriture.
   */
  public Map<String, Object> listImportCandidates(String q) {
    if (!legacyReader.available()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "La source historique est temporairement indisponible.");
    }
    List<ClinicContratLegacyRow> rows = legacyReader.listAll();
    String needle = q == null || q.isBlank() ? null : q.trim().toLowerCase();
    java.util.ArrayList<Map<String, Object>> candidates = new java.util.ArrayList<>();
    java.util.ArrayList<Map<String, Object>> divergenceItems = new java.util.ArrayList<>();
    int already = 0;
    int unchanged = 0;
    int divergences = 0;
    for (ClinicContratLegacyRow row : rows) {
      String code;
      try {
        code = ErpContratRepository.normalizeCode(row.code());
      } catch (IllegalArgumentException ex) {
        continue;
      }
      if (code == null || code.isBlank()) {
        continue;
      }
      String designation = row.designation() == null ? "" : row.designation().trim();
      if (needle != null
          && !code.toLowerCase().contains(needle)
          && !designation.toLowerCase().contains(needle)) {
        continue;
      }
      var erp = repository.findByCode(code);
      if (erp.isEmpty()) {
        Map<String, Object> item = new HashMap<>();
        item.put("id", code);
        item.put("code", code);
        item.put("designation", designation);
        item.put("state", "NEW");
        candidates.add(item);
      } else {
        already++;
        String ed = erp.get().designation() == null ? "" : erp.get().designation().trim();
        if (designation.equals(ed)) {
          unchanged++;
        } else {
          divergences++;
          Map<String, Object> item = new HashMap<>();
          item.put("id", code);
          item.put("code", code);
          item.put("designation", designation);
          item.put("erpDesignation", ed);
          item.put("state", "DIVERGENCE");
          item.put("erpActif", erp.get().actif());
          divergenceItems.add(item);
        }
      }
    }
    candidates.sort((a, b) -> String.valueOf(a.get("code")).compareToIgnoreCase(String.valueOf(b.get("code"))));
    divergenceItems.sort((a, b) -> String.valueOf(a.get("code")).compareToIgnoreCase(String.valueOf(b.get("code"))));
    Map<String, Object> report = new HashMap<>();
    report.put("entity", "TCONTRAT");
    report.put("sourceLabel", "CLINIQUE Beach");
    report.put("targetLabel", "CMK ERP");
    report.put("sourceTotal", rows.size());
    report.put("alreadyImported", already);
    report.put("available", candidates.size());
    report.put("unchanged", unchanged);
    report.put("divergences", divergences);
    report.put("candidates", candidates);
    report.put("divergenceItems", divergenceItems);
    return report;
  }

  /**
   * Import sélectif : relit CLINIQUE, vérifie l'absence MySQL, insert uniquement les codes demandés.
   * Aucune écriture SQL Server, aucun écrasement, aucune réactivation.
   */
  @Transactional
  public Map<String, Object> importSelected(List<String> requestedCodes) {
    if (!legacyReader.available()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "La source historique est temporairement indisponible.");
    }
    if (requestedCodes == null || requestedCodes.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun contrat sélectionné.");
    }
    java.util.LinkedHashSet<String> wanted = new java.util.LinkedHashSet<>();
    for (String raw : requestedCodes) {
      try {
        String c = ErpContratRepository.normalizeCode(raw);
        if (c != null && !c.isBlank()) {
          wanted.add(c);
        }
      } catch (IllegalArgumentException ignored) {
        // code invalide ignoré
      }
    }
    if (wanted.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun code valide sélectionné.");
    }
    Map<String, ClinicContratLegacyRow> byCode = new HashMap<>();
    for (ClinicContratLegacyRow row : legacyReader.listAll()) {
      try {
        String c = ErpContratRepository.normalizeCode(row.code());
        if (c != null && wanted.contains(c)) {
          byCode.put(c, row);
        }
      } catch (IllegalArgumentException ignored) {
        // ignore
      }
    }
    int imported = 0;
    int skipped = 0;
    int failed = 0;
    java.util.ArrayList<String> importedCodes = new java.util.ArrayList<>();
    java.util.ArrayList<String> skippedCodes = new java.util.ArrayList<>();
    for (String code : wanted) {
      ClinicContratLegacyRow row = byCode.get(code);
      if (row == null) {
        failed++;
        continue;
      }
      try {
        int n = repository.insertIfAbsentFromImport(row.code(), row.designation());
        if (n > 0) {
          imported++;
          importedCodes.add(code);
        } else {
          skipped++;
          skippedCodes.add(code);
        }
      } catch (Exception ex) {
        failed++;
      }
    }
    Map<String, Object> report = new HashMap<>();
    report.put("entity", "TCONTRAT");
    report.put("requested", wanted.size());
    report.put("imported", imported);
    report.put("skipped", skipped);
    report.put("failed", failed);
    report.put("importedCodes", importedCodes);
    report.put("skippedCodes", skippedCodes);
    report.put("origin", "IMPORT_CLINIQUE_SELECTIVE");
    return report;
  }

  @Transactional
  public Map<String, Object> importFromClinic() {
    Map<String, Object> candidates = listImportCandidates(null);
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> items = (List<Map<String, Object>>) candidates.get("candidates");
    java.util.ArrayList<String> codes = new java.util.ArrayList<>();
    if (items != null) {
      for (Map<String, Object> item : items) {
        codes.add(String.valueOf(item.get("code")));
      }
    }
    Map<String, Object> selective = importSelected(codes);
    Map<String, Object> report = new HashMap<>(selective);
    report.put("dryRun", false);
    report.put("analyzed", candidates.get("sourceTotal"));
    report.put("upserts", selective.get("imported"));
    return report;
  }

  public Map<String, Object> syncStatus() {
    Map<String, Object> status = new HashMap<>();
    status.put("beachAvailable", legacyReader.available());
    status.put("beachWriteEnabled", writeGuard.beachWriteEnabled());
    status.put("beachSyncEnabled", writeGuard.beachSyncEnabled());
    status.put("beachDeleteEnabled", writeGuard.beachDeleteEnabled());
    status.put("chWriteEnabled", writeGuard.chWriteEnabled());
    status.put("erpContrats", repository.count(null, null));
    status.put("outboxPending", repository.countOutboxByStatus("PENDING"));
    status.put("outboxFailed", repository.countOutboxByStatus("FAILED"));
    status.put("outboxConflict", repository.countOutboxByStatus("CONFLICT"));
    status.put("outboxSynced", repository.countOutboxByStatus("SYNCED"));
    if (legacyReader.available()) {
      try {
        status.put("legacyContrats", legacyReader.count());
      } catch (Exception ex) {
        status.put("legacyContrats", null);
        status.put("legacyError", ex.getMessage());
      }
    } else {
      status.put("legacyContrats", null);
    }
    return status;
  }

  private void enqueueReplication(long id, String code, String designation, String op, Long userId) {
    writeGuard.assertChWriteForbidden();
    writeGuard.assertBeachWriteAllowed("TCONTRAT." + op);
    try {
      Map<String, Object> payload = Map.of(
          "code", code,
          "designation", designation == null ? "" : designation,
          "operation", op,
          "userId", userId == null ? 0 : userId);
      repository.enqueueOutbox(
          UUID.randomUUID().toString(),
          "TCONTRAT",
          id,
          code,
          op,
          objectMapper.writeValueAsString(payload),
          UUID.randomUUID().toString());
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Sérialisation outbox impossible");
    }
  }
}
