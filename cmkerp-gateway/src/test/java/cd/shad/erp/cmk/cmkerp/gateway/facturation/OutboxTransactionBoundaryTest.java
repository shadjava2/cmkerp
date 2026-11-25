package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Garde-fou documentaire + assertions structurelles sur la frontière transactionnelle.
 *
 * <p>Règle CMK :
 * <ul>
 * <li>Création / MAJ ERP + INSERT outbox = <strong>même</strong> transaction MySQL
 * ({@code @Transactional} sur {@code ErpContratService#create/update}).</li>
 * <li>{@code REQUIRES_NEW} réservé au worker (claim / SYNCED / FAILED / UNCERTAIN).</li>
 * <li>Flush Beach uniquement <strong>après commit</strong> MySQL
 * ({@code scheduleOutboxFlushAfterCommit}).</li>
 * </ul>
 */
class OutboxTransactionBoundaryTest {

  @Test
  void workerUsesRequiresNewConstant() throws Exception {
    var field = ClinicBeachOutboxWorker.class.getDeclaredField("requiresNewTx");
    field.setAccessible(true);
    assertEquals("requiresNewTx", field.getName());
    assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers())
        || field.getType().getName().contains("TransactionTemplate"));
  }

  @Test
  void contratServiceExposesCreateUpdateTransactionalContract() throws Exception {
    var create = ErpContratService.class.getMethod(
        "create", String.class, String.class, Long.class);
    var update = ErpContratService.class.getMethod(
        "update", long.class, String.class, int.class, Long.class);
    assertTrue(create.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class));
    assertTrue(update.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class));
  }

  @Test
  void referentialServiceKeepsCreateUpdateImportInRequiredTransaction() throws Exception {
    var create = ClinicReferentialService.class.getMethod(
        "create", String.class, String.class, java.util.Map.class, Long.class);
    var update = ClinicReferentialService.class.getMethod(
        "update", String.class, long.class, java.util.Map.class, int.class, Long.class);
    var importSelected = ClinicReferentialService.class.getMethod(
        "importSelected", String.class, java.util.List.class);
    assertTrue(create.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class));
    assertTrue(update.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class));
    assertTrue(importSelected.isAnnotationPresent(org.springframework.transaction.annotation.Transactional.class));
  }
}
