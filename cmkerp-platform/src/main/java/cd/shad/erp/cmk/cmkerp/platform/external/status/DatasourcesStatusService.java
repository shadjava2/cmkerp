package cd.shad.erp.cmk.cmkerp.platform.external.status;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sonde les 3 bases. Mediline / CLINIQUE absentes ou down → statut dégradé, jamais d'exception fatale.
 */
@Service
public class DatasourcesStatusService {

  private final DataSource primaryDataSource;
  private final ObjectProvider<DataSource> medilineDataSource;
  private final ObjectProvider<DataSource> cliniqueDataSource;

  @Value("${cmk.datasource.mediline.url:}")
  private String medilineUrl;

  @Value("${cmk.datasource.clinique.url:}")
  private String cliniqueUrl;

  @Value("${cmk.datasource.primary.url:}")
  private String primaryUrl;

  public DatasourcesStatusService(
      @Qualifier("primaryDataSource") DataSource primaryDataSource,
      @Qualifier("medilineDataSource") ObjectProvider<DataSource> medilineDataSource,
      @Qualifier("cliniqueDataSource") ObjectProvider<DataSource> cliniqueDataSource) {
    this.primaryDataSource = primaryDataSource;
    this.medilineDataSource = medilineDataSource;
    this.cliniqueDataSource = cliniqueDataSource;
  }

  public DatasourcesStatusReport checkAll() {
    List<DatasourceStatus> items = new ArrayList<>(3);
    items.add(probe("primary", "cmkerp-v24prod (ERP)", "MySQL", true, true, primaryUrl,
        primaryDataSource));
    items.add(probeOptional("mediline", "Mediline (production)", "MySQL", medilineUrl,
        medilineDataSource.getIfAvailable()));
    items.add(probeOptional("clinique", "CLINIQUE (SQL Server)", "SQL Server", cliniqueUrl,
        cliniqueDataSource.getIfAvailable()));
    return new DatasourcesStatusReport(Instant.now().toString(), false, items);
  }

  private DatasourceStatus probeOptional(String id, String label, String engine, String url,
      DataSource ds) {
    boolean configured = url != null && !url.isBlank();
    if (!configured) {
      return new DatasourceStatus(id, label, engine, false, false, false, "Non configuré (URL vide)",
          null, sanitizeUrl(url));
    }
    if (ds == null) {
      return new DatasourceStatus(id, label, engine, true, false, false,
          "Configuré mais pool absent", null, sanitizeUrl(url));
    }
    return probe(id, label, engine, true, false, url, ds);
  }

  private DatasourceStatus probe(String id, String label, String engine, boolean configured,
      boolean required, String url, DataSource ds) {
    long start = System.currentTimeMillis();
    try (Connection conn = ds.getConnection();
        Statement st = conn.createStatement()) {
      st.setQueryTimeout(5);
      try (ResultSet rs = st.executeQuery("SELECT 1")) {
        rs.next();
      }
      long latency = System.currentTimeMillis() - start;
      return new DatasourceStatus(id, label, engine, configured, true, required, "Connecté", latency,
          sanitizeUrl(url));
    } catch (Exception ex) {
      long latency = System.currentTimeMillis() - start;
      String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
      if (msg.length() > 240) {
        msg = msg.substring(0, 240) + "…";
      }
      return new DatasourceStatus(id, label, engine, configured, false, required, msg, latency,
          sanitizeUrl(url));
    }
  }

  private static String sanitizeUrl(String url) {
    if (url == null || url.isBlank()) {
      return "";
    }
    String safe = url;
    int q = safe.indexOf('?');
    if (q > 0) {
      safe = safe.substring(0, q);
    }
    return safe.length() > 160 ? safe.substring(0, 160) + "…" : safe;
  }

  public record DatasourceStatus(
      String id,
      String label,
      String engine,
      boolean configured,
      boolean connected,
      boolean required,
      String message,
      Long latencyMs,
      String urlHint) {
  }

  public record DatasourcesStatusReport(
      String checkedAt,
      boolean externalCanBlockStartup,
      List<DatasourceStatus> datasources) {
  }
}
