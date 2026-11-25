package cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feature flags de coexistence CLINIQUE. Défauts sûrs : aucune écriture SQL Server nouvelle.
 */
@ConfigurationProperties(prefix = "clinic")
public class ClinicLegacySyncProperties {

  private final Beach beach = new Beach();
  private final Ch ch = new Ch();

  public Beach getBeach() {
    return beach;
  }

  public Ch getCh() {
    return ch;
  }

  public static class Beach {
    /** Active le worker / traitement outbox Beach. */
    private boolean syncEnabled = false;
    /** Autorise INSERT/UPDATE paramétrés vers Beach (hors legacy PAU historique). */
    private boolean writeEnabled = false;
    /** Autorise DELETE physique Beach (exceptionnel). */
    private boolean deleteEnabled = false;

    public boolean isSyncEnabled() {
      return syncEnabled;
    }

    public void setSyncEnabled(boolean syncEnabled) {
      this.syncEnabled = syncEnabled;
    }

    public boolean isWriteEnabled() {
      return writeEnabled;
    }

    public void setWriteEnabled(boolean writeEnabled) {
      this.writeEnabled = writeEnabled;
    }

    public boolean isDeleteEnabled() {
      return deleteEnabled;
    }

    public void setDeleteEnabled(boolean deleteEnabled) {
      this.deleteEnabled = deleteEnabled;
    }
  }

  public static class Ch {
    /** Écritures vers CLINIQUE CH — toujours off pour cette migration. */
    private boolean writeEnabled = false;

    public boolean isWriteEnabled() {
      return writeEnabled;
    }

    public void setWriteEnabled(boolean writeEnabled) {
      this.writeEnabled = writeEnabled;
    }
  }
}
