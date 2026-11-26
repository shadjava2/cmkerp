package cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Garde-fou applicatif des écritures SQL Server CLINIQUE (en complément des privilèges DBA).
 */
@Component
public class ClinicLegacyWriteGuard {

  private final ClinicLegacySyncProperties properties;

  public ClinicLegacyWriteGuard(ClinicLegacySyncProperties properties) {
    this.properties = properties;
  }

  public void assertBeachWriteAllowed(String operation) {
    if (!properties.getBeach().isWriteEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Écriture CLINIQUE Beach désactivée (clinic.beach.write-enabled=false). Opération: " + operation);
    }
  }

  /**
   * Vérifie write-enabled global + allow-list CSV d'entités (jamais {@code *}).
   */
  public void assertBeachEntityWriteAllowed(String entityType, String operation) {
    assertBeachWriteAllowed(operation);
    if (!properties.getBeach().isEntityWriteAllowed(entityType)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Écriture Beach interdite pour l'entité " + entityType + " (clinic.beach.write-entities). Opération: "
              + operation);
    }
  }

  public void assertBeachDeleteAllowed(String operation) {
    assertBeachWriteAllowed(operation);
    if (!properties.getBeach().isDeleteEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Suppression CLINIQUE Beach interdite (clinic.beach.delete.enabled=false). Opération: " + operation);
    }
  }

  public void assertChWriteForbidden() {
    if (properties.getCh().isWriteEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Les écritures CLINIQUE CH sont hors périmètre de cette migration.");
    }
  }

  public boolean beachWriteEnabled() {
    return properties.getBeach().isWriteEnabled();
  }

  public boolean beachEntityWriteEnabled(String entityType) {
    return properties.getBeach().isEntityWriteAllowed(entityType);
  }

  public boolean beachSyncEnabled() {
    return properties.getBeach().isSyncEnabled();
  }

  public boolean beachInboundEnabled() {
    return properties.getBeach().isInboundEnabled();
  }

  public boolean beachDeleteEnabled() {
    return properties.getBeach().isDeleteEnabled();
  }

  public boolean chWriteEnabled() {
    return properties.getCh().isWriteEnabled();
  }

  public ClinicLegacySyncProperties properties() {
    return properties;
  }
}
