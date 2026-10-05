package cd.shad.erp.cmk.cmkerp.platform.config.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Log de démarrage des datasources externes (sans mot de passe).
 */
@Component
public class ExternalDataSourceStartupLogger {

  private static final Logger log = LoggerFactory.getLogger(ExternalDataSourceStartupLogger.class);

  @Value("${cmk.datasource.mediline.url:}")
  private String medilineUrl;

  @Value("${cmk.datasource.clinique.url:}")
  private String cliniqueUrl;

  @EventListener(ApplicationReadyEvent.class)
  public void logExternalDatasources() {
    logDatasource("Mediline (MySQL production)", medilineUrl);
    logDatasource("CLINIQUE (SQL Server)", cliniqueUrl);
  }

  private static void logDatasource(String label, String url) {
    if (url == null || url.isBlank()) {
      log.info("{} : non configuré (stubs actifs, démarrage OK)", label);
      return;
    }
    String safe = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
    int semi = safe.indexOf(';');
    if (semi > 0) {
      // SQL Server : garder host + instance/databaseName sans params auth
      String base = safe.substring(0, Math.min(safe.length(), 120));
      log.info("{} : configuré -> {}", label, base);
    } else {
      log.info("{} : configuré -> {}", label, safe);
    }
  }
}
