package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;

/**
 * Worker ERP → CLINIQUE Beach. SYNCED uniquement après relecture SQL Server.
 */
@Component
@ConditionalOnProperty(prefix = "clinic.beach", name = "sync-enabled", havingValue = "true")
public class ClinicBeachOutboxWorker {

  private static final Logger log = LoggerFactory.getLogger(ClinicBeachOutboxWorker.class);
  private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
  };

  private final ErpContratRepository repository;
  private final ClinicReferentialRepository referentialRepository;
  private final ClinicReferentialLegacyWriter referentialWriter;
  private final ClinicLegacyWriteGuard writeGuard;
  private final ObjectMapper objectMapper;
  private final JdbcTemplate cliniqueJdbc;

  public ClinicBeachOutboxWorker(
      ErpContratRepository repository,
      ClinicReferentialRepository referentialRepository,
      ClinicReferentialLegacyWriter referentialWriter,
      ClinicLegacyWriteGuard writeGuard,
      ObjectMapper objectMapper,
      @Autowired(required = false) @Qualifier("cliniqueJdbcTemplate") JdbcTemplate cliniqueJdbc) {
    this.repository = repository;
    this.referentialRepository = referentialRepository;
    this.referentialWriter = referentialWriter;
    this.writeGuard = writeGuard;
    this.objectMapper = objectMapper;
    this.cliniqueJdbc = cliniqueJdbc;
  }

  @Scheduled(fixedDelayString = "${clinic.beach.sync-interval-ms:15000}")
  public void process() {
    if (!writeGuard.beachSyncEnabled()) {
      return;
    }
    writeGuard.assertChWriteForbidden();

    var beach = writeGuard.properties().getBeach();
    repository.reclaimStuckInProgress(beach.getStuckMinutes());
    repository.requeueFailedReady(beach.getMaxAttempts());

    if (!writeGuard.beachWriteEnabled()) {
      log.debug("Worker Beach : écritures désactivées — SUSPENDED / UNCERTAIN conservés");
      return;
    }
    if (cliniqueJdbc == null) {
      log.warn("Worker Beach : datasource CLINIQUE indisponible");
      return;
    }

    // Promotion uniquement pour entités autorisées (TCONTRAT).
    for (String entity : beach.writeEntitySet()) {
      int promoted = repository.promoteSuspendedToPendingForEntity(entity);
      if (promoted > 0) {
        log.info("Worker Beach : {} intention(s) {} SUSPENDED → PENDING", promoted, entity);
      }
    }

    // Relecture UNCERTAIN TCONTRAT avant nouvelle écriture.
    reconcileUncertainTcontrat();

    List<Map<String, Object>> batch = repository.claimPendingOutbox(20);
    for (Map<String, Object> row : batch) {
      long id = ((Number) row.get("id")).longValue();
      String entity = String.valueOf(row.get("entityType"));
      try {
        if (!writeGuard.beachEntityWriteEnabled(entity)) {
          repository.markOutboxFailed(id, "Entité non autorisée à l'écriture: " + entity);
          continue;
        }
        if ("TCONTRAT".equalsIgnoreCase(entity)) {
          applyTcontrat(row);
          verifyAndCompleteTcontrat(row);
        } else {
          applyReferential(row, entity);
          repository.markOutboxSynced(id);
          Object entityId = row.get("entityId");
          if (entityId instanceof Number n) {
            referentialRepository.markSynced(n.longValue(), String.valueOf(row.get("legacyKey")));
          }
        }
      } catch (Exception ex) {
        log.warn("Worker Beach échec op={} : {}", row.get("operationId"), ex.getMessage());
        if (looksUncertain(ex)) {
          repository.markOutboxUncertain(id, ex.getMessage());
        } else {
          repository.markOutboxFailed(id, ex.getMessage());
        }
      }
    }
  }

  private void reconcileUncertainTcontrat() {
    List<Map<String, Object>> uncertain = repository.listOutbox("UNCERTAIN", 20, 0);
    for (Map<String, Object> row : uncertain) {
      if (!"TCONTRAT".equalsIgnoreCase(String.valueOf(row.get("entityType")))) {
        continue;
      }
      long id = ((Number) row.get("id")).longValue();
      String code = String.valueOf(row.get("legacyKey"));
      try {
        var beachDes = repository.findBeachDesignationVerified(cliniqueJdbc, code);
        if (beachDes.isEmpty()) {
          // Pas trouvé — retenter plus tard
          repository.markOutboxFailed(id, "UNCERTAIN: absent de CLINIQUE après écriture");
          continue;
        }
        // Payload non disponible dans listOutbox — basculer PENDING pour re-vérification via claim
        // On marque SYNCED si une désignation existe (preuve d'écriture) et aligne ERP.
        Object entityId = row.get("entityId");
        if (entityId instanceof Number n) {
          String fp = ClinicContratFingerprint.of(code, beachDes.get());
          repository.markContratSyncedWithFingerprint(n.longValue(), code, fp);
        }
        repository.markOutboxSynced(id);
      } catch (Exception ex) {
        log.debug("UNCERTAIN TCONTRAT {} : {}", code, ex.getMessage());
      }
    }
  }

  private void applyTcontrat(Map<String, Object> row) throws Exception {
    writeGuard.assertBeachEntityWriteAllowed("TCONTRAT", "TCONTRAT." + row.get("operationType"));
    JsonNode payload = objectMapper.readTree(String.valueOf(row.get("payloadJson")));
    String code = payload.path("code").asText(null);
    String designation = payload.path("designation").asText("");
    if (code == null || code.isBlank()) {
      throw new IllegalStateException("Payload sans code");
    }
    String op = String.valueOf(row.get("operationType"));
    Integer exists = cliniqueJdbc.query(
        "SELECT COUNT(*) FROM dbo.TCONTRAT WHERE RTRIM(CODE) = ?",
        rs -> rs.next() ? rs.getInt(1) : 0,
        code);
    if ("INSERT".equalsIgnoreCase(op)) {
      if (exists != null && exists > 0) {
        cliniqueJdbc.update(
            "UPDATE dbo.TCONTRAT SET DESIGNATION = ? WHERE RTRIM(CODE) = ?",
            designation,
            code);
      } else {
        cliniqueJdbc.update(
            "INSERT INTO dbo.TCONTRAT (CODE, DESIGNATION) VALUES (?, ?)",
            code,
            designation);
      }
      return;
    }
    if ("UPDATE".equalsIgnoreCase(op)) {
      int n = cliniqueJdbc.update(
          "UPDATE dbo.TCONTRAT SET DESIGNATION = ? WHERE RTRIM(CODE) = ?",
          designation,
          code);
      if (n == 0) {
        cliniqueJdbc.update(
            "INSERT INTO dbo.TCONTRAT (CODE, DESIGNATION) VALUES (?, ?)",
            code,
            designation);
      }
      return;
    }
    throw new IllegalStateException("Opération non supportée: " + op);
  }

  private void verifyAndCompleteTcontrat(Map<String, Object> row) throws Exception {
    long id = ((Number) row.get("id")).longValue();
    JsonNode payload = objectMapper.readTree(String.valueOf(row.get("payloadJson")));
    String code = payload.path("code").asText(null);
    String expected = payload.path("designation").asText("");
    var actual = repository.findBeachDesignationVerified(cliniqueJdbc, code);
    if (actual.isEmpty()) {
      repository.markOutboxUncertain(id, "Écriture non confirmée dans CLINIQUE");
      return;
    }
    if (!Objects.equals(normalizeDes(expected), normalizeDes(actual.get()))) {
      repository.markOutboxConflict(id, "Désignation CLINIQUE différente après écriture");
      return;
    }
    repository.markOutboxSynced(id);
    Object entityId = row.get("entityId");
    if (entityId instanceof Number n) {
      repository.markContratSyncedWithFingerprint(
          n.longValue(), code, ClinicContratFingerprint.of(code, actual.get()));
    }
  }

  private void applyReferential(Map<String, Object> row, String entity) throws Exception {
    writeGuard.assertBeachEntityWriteAllowed(entity, entity + "." + row.get("operationType"));
    Definition def = ClinicReferentialCatalog.byEntity(entity)
        .orElseThrow(() -> new IllegalStateException("Référentiel non pris en charge"));
    JsonNode payload = objectMapper.readTree(String.valueOf(row.get("payloadJson")));
    String key = payload.path("key").asText(null);
    if (key == null || key.isBlank()) {
      throw new IllegalStateException("Payload sans identifiant");
    }
    Map<String, Object> fields = new LinkedHashMap<>();
    JsonNode fieldsNode = payload.path("fields");
    if (fieldsNode.isObject()) {
      fields.putAll(objectMapper.convertValue(fieldsNode, MAP_TYPE));
    }
    fields.remove("MOTDEPASSE");
    fields.remove("motdepasse");
    fields.remove("ACTIF");
    fields.remove("actif");
    referentialWriter.apply(def, String.valueOf(row.get("operationType")), key, fields);
  }

  private static boolean looksUncertain(Exception ex) {
    String m = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
    return m.contains("timeout") || m.contains("connection") || m.contains("broken");
  }

  private static String normalizeDes(String v) {
    return v == null ? "" : v.trim();
  }
}
