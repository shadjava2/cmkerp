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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cd.shad.erp.cmk.cmkerp.gateway.facturation.ClinicReferentialCatalog.Definition;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;

/**
 * Worker ERP → CLINIQUE Beach. SYNCED uniquement après relecture SQL Server.
 *
 * <p>Les mises à jour MySQL (claim / SYNCED / FAILED) passent en {@code REQUIRES_NEW}
 * pour ne pas être annulées par un rollback de transaction ambiante (écriture Beach
 * réussie sinon sans bascule de statut outbox).
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
  private final ErpFacturationRelationalStore relationalStore;
  private final ClinicLegacyWriteGuard writeGuard;
  private final ObjectMapper objectMapper;
  private final JdbcTemplate cliniqueJdbc;
  private final TransactionTemplate requiresNewTx;

  public ClinicBeachOutboxWorker(
      ErpContratRepository repository,
      ClinicReferentialRepository referentialRepository,
      ClinicReferentialLegacyWriter referentialWriter,
      ErpFacturationRelationalStore relationalStore,
      ClinicLegacyWriteGuard writeGuard,
      ObjectMapper objectMapper,
      PlatformTransactionManager transactionManager,
      @Autowired(required = false) @Qualifier("cliniqueJdbcTemplate") JdbcTemplate cliniqueJdbc) {
    this.repository = repository;
    this.referentialRepository = referentialRepository;
    this.referentialWriter = referentialWriter;
    this.relationalStore = relationalStore;
    this.writeGuard = writeGuard;
    this.objectMapper = objectMapper;
    this.cliniqueJdbc = cliniqueJdbc;
    this.requiresNewTx = new TransactionTemplate(transactionManager);
    this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() {
    try {
      int n = flushPendingBatch(50);
      if (n > 0) {
        log.info("Worker Beach : {} intention(s) traitée(s) au démarrage", n);
      }
    } catch (Exception ex) {
      log.warn("Worker Beach démarrage : {}", ex.getMessage());
    }
  }

  @Scheduled(fixedDelayString = "${clinic.beach.sync-interval-ms:15000}")
  public void process() {
    flushPendingBatch(20);
  }

  /**
   * Traite immédiatement un lot PENDING (appelé aussi après Valider rapprochement).
   */
  public int flushPendingBatch(int limit) {
    if (!writeGuard.beachSyncEnabled()) {
      return 0;
    }
    writeGuard.assertChWriteForbidden();

    var beach = writeGuard.properties().getBeach();
    requiresNewTx.executeWithoutResult(status -> {
      repository.reclaimStuckInProgress(beach.getStuckMinutes());
      repository.requeueFailedReady(beach.getMaxAttempts());
    });

    if (!writeGuard.beachWriteEnabled()) {
      log.debug("Worker Beach : écritures désactivées — SUSPENDED / UNCERTAIN conservés");
      return 0;
    }
    if (cliniqueJdbc == null) {
      log.warn("Worker Beach : datasource CLINIQUE indisponible");
      return 0;
    }

    requiresNewTx.executeWithoutResult(status -> {
      for (String entity : beach.writeEntitySet()) {
        int promoted = repository.promoteSuspendedToPendingForEntity(entity);
        if (promoted > 0) {
          log.info("Worker Beach : {} intention(s) {} SUSPENDED → PENDING", promoted, entity);
        }
      }
    });

    reconcileUncertainTcontrat();

    List<Map<String, Object>> batch = requiresNewTx.execute(
        status -> repository.claimPendingOutbox(Math.min(Math.max(limit, 1), 50)));
    if (batch == null || batch.isEmpty()) {
      return 0;
    }
    log.info("Worker Beach : {} intention(s) PENDING prises en charge", batch.size());
    int done = 0;
    for (Map<String, Object> row : batch) {
      long id = ((Number) row.get("id")).longValue();
      String entity = String.valueOf(row.get("entityType"));
      try {
        if (!writeGuard.beachEntityWriteEnabled(entity)) {
          requiresNewTx.executeWithoutResult(
              status -> repository.markOutboxFailed(id, "Entité non autorisée à l'écriture: " + entity));
          continue;
        }
        if ("TCONTRAT".equalsIgnoreCase(entity)) {
          applyTcontrat(row);
          requiresNewTx.executeWithoutResult(status -> verifyAndCompleteTcontrat(row));
        } else if ("TAYANTDROIT".equalsIgnoreCase(entity)) {
          applyReferential(row, entity);
          requiresNewTx.executeWithoutResult(status -> verifyAndCompleteAyant(row));
        } else {
          applyReferential(row, entity);
          requiresNewTx.executeWithoutResult(status -> {
            repository.markOutboxSynced(id);
            Object entityId = row.get("entityId");
            if (entityId instanceof Number n) {
              referentialRepository.markSynced(n.longValue(), String.valueOf(row.get("legacyKey")));
            }
          });
        }
        done++;
      } catch (Exception ex) {
        log.warn("Worker Beach échec op={} key={} : {}", row.get("operationId"), row.get("legacyKey"),
            ex.getMessage());
        requiresNewTx.executeWithoutResult(status -> {
          if (looksUncertain(ex)) {
            repository.markOutboxUncertain(id, ex.getMessage());
          } else {
            repository.markOutboxFailed(id, ex.getMessage());
          }
        });
      }
    }
    return done;
  }

  private void reconcileUncertainTcontrat() {
    List<Map<String, Object>> uncertain = requiresNewTx.execute(
        status -> repository.listOutbox("UNCERTAIN", 20, 0));
    if (uncertain == null) {
      return;
    }
    for (Map<String, Object> row : uncertain) {
      if (!"TCONTRAT".equalsIgnoreCase(String.valueOf(row.get("entityType")))) {
        continue;
      }
      long id = ((Number) row.get("id")).longValue();
      String code = String.valueOf(row.get("legacyKey"));
      try {
        var beachDes = repository.findBeachDesignationVerified(cliniqueJdbc, code);
        if (beachDes.isEmpty()) {
          requiresNewTx.executeWithoutResult(
              status -> repository.markOutboxFailed(id, "UNCERTAIN: absent de CLINIQUE après écriture"));
          continue;
        }
        requiresNewTx.executeWithoutResult(status -> {
          Object entityId = row.get("entityId");
          if (entityId instanceof Number n) {
            String fp = ClinicContratFingerprint.of(code, beachDes.get());
            repository.markContratSyncedWithFingerprint(n.longValue(), code, fp);
          }
          repository.markOutboxSynced(id);
        });
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

  private void verifyAndCompleteAyant(Map<String, Object> row) {
    long outboxId = ((Number) row.get("id")).longValue();
    try {
      if (cliniqueJdbc == null) {
        repository.markOutboxUncertain(outboxId, "Datasource CLINIQUE Beach indisponible");
        return;
      }
      String key = String.valueOf(row.get("legacyKey"));
      Integer exists = cliniqueJdbc.query(
          "SELECT COUNT(*) FROM dbo.TAYANTDROIT WHERE RTRIM(MATRICULE) = ?",
          rs -> rs.next() ? rs.getInt(1) : 0,
          key);
      if (exists == null || exists <= 0) {
        repository.markOutboxUncertain(outboxId, "Écriture TAYANTDROIT non confirmée dans CLINIQUE");
        return;
      }
      repository.markOutboxSynced(outboxId);
      Object entityId = row.get("entityId");
      if (entityId instanceof Number n) {
        relationalStore.markAyantSynced(n.longValue(), null);
      }
    } catch (Exception ex) {
      repository.markOutboxUncertain(outboxId, ex.getMessage());
    }
  }

  private void verifyAndCompleteTcontrat(Map<String, Object> row) {
    long id = ((Number) row.get("id")).longValue();
    try {
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
    } catch (Exception ex) {
      repository.markOutboxUncertain(id, ex.getMessage());
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
