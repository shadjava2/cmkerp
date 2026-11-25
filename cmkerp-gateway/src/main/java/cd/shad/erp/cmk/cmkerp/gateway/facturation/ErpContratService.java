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
    if (!legacyReader.available()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Datasource CLINIQUE Beach indisponible");
    }
    List<ClinicContratLegacyRow> rows = legacyReader.listAll();
    int neu = 0;
    int existing = 0;
    int unchanged = 0;
    int modified = 0;
    for (ClinicContratLegacyRow row : rows) {
      var erp = repository.findByCode(row.code());
      if (erp.isEmpty()) {
        neu++;
      } else {
        existing++;
        String d = row.designation() == null ? "" : row.designation().trim();
        String ed = erp.get().designation() == null ? "" : erp.get().designation().trim();
        if (d.equals(ed)) {
          unchanged++;
        } else {
          modified++;
        }
      }
    }
    Map<String, Object> report = new HashMap<>();
    report.put("entity", "TCONTRAT");
    report.put("dryRun", true);
    report.put("analyzed", rows.size());
    report.put("newRows", neu);
    report.put("existingRows", existing);
    report.put("unchanged", unchanged);
    report.put("modified", modified);
    report.put("writesApplied", 0);
    return report;
  }

  @Transactional
  public Map<String, Object> importFromClinic() {
    if (!legacyReader.available()) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Datasource CLINIQUE Beach indisponible");
    }
    List<ClinicContratLegacyRow> rows = legacyReader.listAll();
    int applied = 0;
    for (ClinicContratLegacyRow row : rows) {
      if (row.code() == null || row.code().isBlank()) {
        continue;
      }
      applied += repository.upsertFromImport(row.code(), row.designation());
    }
    Map<String, Object> report = new HashMap<>();
    report.put("entity", "TCONTRAT");
    report.put("dryRun", false);
    report.put("analyzed", rows.size());
    report.put("upserts", applied);
    report.put("origin", "IMPORT_CLINIQUE");
    report.put("note", "Import MySQL uniquement — aucune écriture retour vers CLINIQUE");
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
