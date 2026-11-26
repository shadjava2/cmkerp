package cd.shad.erp.cmk.cmkerp.platform.external.clinique.sync;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
    /** Active le worker outbox / inbound Beach. */
    private boolean syncEnabled = false;
    /** Autorise INSERT/UPDATE paramétrés vers Beach (toujours gated par writeEntities). */
    private boolean writeEnabled = false;
    /** Entités autorisées à l'écriture Beach lorsque writeEnabled=true (ex. TCONTRAT). */
    /** Allow-list CSV explicite — jamais « * ». Défaut minimal sûr ; override env en recette/prod. */
    private String writeEntities = "TCONTRAT,TAYANTDROIT";
    /** Autorise DELETE physique Beach (exceptionnel). */
    private boolean deleteEnabled = false;
    /** Active le rapprochement entrant périodique CLINIQUE → ERP. */
    private boolean inboundEnabled = false;
    /** Intervalle du rapprochement entrant (ms). */
    private long inboundIntervalMs = 60_000L;
    /** Intervalle du worker outbox (ms). */
    private long syncIntervalMs = 15_000L;
    /** Tentatives max avant abandon (FAILED terminal jusqu'à relance manuelle). */
    private int maxAttempts = 8;
    /** Minutes avant reprise d'une opération IN_PROGRESS orpheline. */
    private int stuckMinutes = 10;
    /** Codes exclus du rattrapage automatique (données de test). */
    private String excludeCodes = "TES";

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

    public String getWriteEntities() {
      return writeEntities;
    }

    public void setWriteEntities(String writeEntities) {
      this.writeEntities = writeEntities;
    }

    public boolean isDeleteEnabled() {
      return deleteEnabled;
    }

    public void setDeleteEnabled(boolean deleteEnabled) {
      this.deleteEnabled = deleteEnabled;
    }

    public boolean isInboundEnabled() {
      return inboundEnabled;
    }

    public void setInboundEnabled(boolean inboundEnabled) {
      this.inboundEnabled = inboundEnabled;
    }

    public long getInboundIntervalMs() {
      return inboundIntervalMs;
    }

    public void setInboundIntervalMs(long inboundIntervalMs) {
      this.inboundIntervalMs = inboundIntervalMs;
    }

    public long getSyncIntervalMs() {
      return syncIntervalMs;
    }

    public void setSyncIntervalMs(long syncIntervalMs) {
      this.syncIntervalMs = syncIntervalMs;
    }

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public int getStuckMinutes() {
      return stuckMinutes;
    }

    public void setStuckMinutes(int stuckMinutes) {
      this.stuckMinutes = stuckMinutes;
    }

    public String getExcludeCodes() {
      return excludeCodes;
    }

    public void setExcludeCodes(String excludeCodes) {
      this.excludeCodes = excludeCodes;
    }

    public Set<String> writeEntitySet() {
      return splitCsv(writeEntities);
    }

    public Set<String> excludeCodeSet() {
      return splitCsv(excludeCodes);
    }

    public boolean isEntityWriteAllowed(String entityType) {
      if (!writeEnabled || entityType == null || entityType.isBlank()) {
        return false;
      }
      Set<String> allowed = writeEntitySet();
      return allowed.isEmpty() || allowed.contains(entityType.trim().toUpperCase(Locale.ROOT));
    }

    private static Set<String> splitCsv(String raw) {
      if (raw == null || raw.isBlank()) {
        return Set.of();
      }
      return Arrays.stream(raw.split(","))
          .map(String::trim)
          .filter(s -> !s.isEmpty())
          .map(s -> s.toUpperCase(Locale.ROOT))
          .collect(Collectors.toCollection(LinkedHashSet::new));
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

  public List<String> effectiveCapabilities() {
    List<String> caps = new ArrayList<>();
    caps.add("READ_CLINIQUE");
    if (beach.isInboundEnabled() && beach.isSyncEnabled()) {
      caps.add("INBOUND_RECONCILE");
    }
    if (beach.isWriteEnabled()) {
      caps.addAll(beach.writeEntitySet().stream().map(e -> "WRITE_" + e).toList());
    }
    if (beach.isDeleteEnabled()) {
      caps.add("DELETE_BEACH");
    }
    return caps;
  }
}
