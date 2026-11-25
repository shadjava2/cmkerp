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
          "Écriture CLINIQUE Beach désactivée (clinic.beach.write.enabled=false). Opération: " + operation);
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
      // même si un flag malveillant est activé dans une config non validée, bloquer par défaut métier migration
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Les écritures CLINIQUE CH sont hors périmètre de cette migration.");
    }
  }

  public boolean beachWriteEnabled() {
    return properties.getBeach().isWriteEnabled();
  }

  public boolean beachSyncEnabled() {
    return properties.getBeach().isSyncEnabled();
  }

  public boolean beachDeleteEnabled() {
    return properties.getBeach().isDeleteEnabled();
  }

  public boolean chWriteEnabled() {
    return properties.getCh().isWriteEnabled();
  }
}
