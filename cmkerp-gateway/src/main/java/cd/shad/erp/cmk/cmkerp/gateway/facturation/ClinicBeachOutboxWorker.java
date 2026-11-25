package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;

/**
 * Worker de réplication ERP → CLINIQUE Beach (TCONTRAT CODE/DESIGNATION uniquement).
 *
 * <p>Inactif tant que {@code clinic.beach.sync-enabled=false} ou écritures désactivées.
 * Les intentions SUSPENDED sont promues en PENDING uniquement lorsque les écritures sont autorisées.
 */
@Component
@ConditionalOnProperty(prefix = "clinic.beach", name = "sync-enabled", havingValue = "true")
public class ClinicBeachOutboxWorker {

  private static final Logger log = LoggerFactory.getLogger(ClinicBeachOutboxWorker.class);

  private final ErpContratRepository repository;
  private final ClinicLegacyWriteGuard writeGuard;
  private final ObjectMapper objectMapper;
  private final JdbcTemplate cliniqueJdbc;

  public ClinicBeachOutboxWorker(
      ErpContratRepository repository,
      ClinicLegacyWriteGuard writeGuard,
      ObjectMapper objectMapper,
      @Autowired(required = false) @Qualifier("cliniqueJdbcTemplate") JdbcTemplate cliniqueJdbc) {
    this.repository = repository;
    this.writeGuard = writeGuard;
    this.objectMapper = objectMapper;
    this.cliniqueJdbc = cliniqueJdbc;
  }

  @Scheduled(fixedDelayString = "${clinic.beach.sync-interval-ms:15000}")
  public void process() {
    if (!writeGuard.beachSyncEnabled()) {
      return;
    }
    if (!writeGuard.beachWriteEnabled()) {
      log.debug("Worker Beach : écritures désactivées — intentions SUSPENDED conservées");
      return;
    }
    writeGuard.assertChWriteForbidden();
    int promoted = repository.promoteSuspendedToPending();
    if (promoted > 0) {
      log.info("Worker Beach : {} intention(s) SUSPENDED → PENDING", promoted);
    }
    if (cliniqueJdbc == null) {
      log.warn("Worker Beach : datasource CLINIQUE indisponible");
      return;
    }
    List<Map<String, Object>> batch = repository.claimPendingOutbox(20);
    for (Map<String, Object> row : batch) {
      long id = ((Number) row.get("id")).longValue();
      try {
        apply(row);
        repository.markOutboxSynced(id);
        Object entityId = row.get("entityId");
        if (entityId instanceof Number n) {
          repository.markContratSynced(n.longValue(), String.valueOf(row.get("legacyKey")));
        }
      } catch (Exception ex) {
        log.warn("Worker Beach échec op={} : {}", row.get("operationId"), ex.getMessage());
        repository.markOutboxFailed(id, ex.getMessage());
      }
    }
  }

  private void apply(Map<String, Object> row) throws Exception {
    writeGuard.assertBeachWriteAllowed("TCONTRAT." + row.get("operationType"));
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
}
