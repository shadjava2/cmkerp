package cd.shad.erp.cmk.cmkerp.gateway.facturation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync.ClinicLegacyWriteGuard;

/**
 * Rapprochement périodique CLINIQUE → ERP pour TCONTRAT (empreinte CODE|DESIGNATION).
 * Aucune écriture SQL Server. Aucune boucle outbox.
 */
@Component
@ConditionalOnProperty(prefix = "clinic.beach", name = "sync-enabled", havingValue = "true")
public class ClinicContratInboundWorker {

  private static final Logger log = LoggerFactory.getLogger(ClinicContratInboundWorker.class);

  private final ClinicContratSyncEngine syncEngine;
  private final ClinicLegacyWriteGuard writeGuard;

  public ClinicContratInboundWorker(ClinicContratSyncEngine syncEngine, ClinicLegacyWriteGuard writeGuard) {
    this.syncEngine = syncEngine;
    this.writeGuard = writeGuard;
  }

  @Scheduled(fixedDelayString = "${clinic.beach.inbound-interval-ms:60000}")
  public void reconcile() {
    if (!writeGuard.beachSyncEnabled() || !writeGuard.beachInboundEnabled()) {
      return;
    }
    try {
      syncEngine.runInboundReconcile(true);
    } catch (Exception ex) {
      log.warn("Inbound TCONTRAT échoué : {}", ex.getMessage());
    }
  }
}
