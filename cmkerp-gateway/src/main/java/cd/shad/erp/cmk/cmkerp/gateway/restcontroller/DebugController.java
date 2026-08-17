package cd.shad.erp.cmk.cmkerp.gateway.restcontroller;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Contrôleur de debug pour tester la connexion aux bases de données.
 *
 * <p>
 * Endpoints disponibles :
 * <ul>
 * <li>GET /debug/db : Test de connexion à la datasource primaire</li>
 * </ul>
 */
@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
@Slf4j
public class DebugController {

  @Qualifier("primaryJdbcTemplate")
  private final JdbcTemplate primaryJdbcTemplate;

  @Qualifier("primaryDataSource")
  private final DataSource primaryDataSource;

  /**
   * Test de connexion à la base de données primaire.
   *
   * <p>
   * Exécute un SELECT 1 pour vérifier que la connexion fonctionne.
   * Retourne également les informations de la datasource Hikari.
   *
   * @return Map contenant le statut de la connexion et les informations de la datasource
   */
  @GetMapping("/db")
  public ResponseEntity<Map<String, Object>> testDatabase() {
    Map<String, Object> result = new HashMap<>();

    try {
      // Test de connexion avec SELECT 1
      Integer testResult = primaryJdbcTemplate.queryForObject("SELECT 1", Integer.class);
      result.put("status", "UP");
      result.put("connectionTest", testResult != null && testResult == 1 ? "OK" : "FAILED");

      // Informations de la datasource Hikari
      if (primaryDataSource instanceof HikariDataSource hikari) {
        Map<String, Object> hikariInfo = new HashMap<>();
        hikariInfo.put("jdbcUrl", hikari.getJdbcUrl());
        hikariInfo.put("poolName", hikari.getPoolName());
        hikariInfo.put("maximumPoolSize", hikari.getMaximumPoolSize());
        hikariInfo.put("minimumIdle", hikari.getMinimumIdle());
        hikariInfo.put("activeConnections", hikari.getHikariPoolMXBean() != null
            ? hikari.getHikariPoolMXBean().getActiveConnections()
            : "N/A");
        hikariInfo.put("idleConnections", hikari.getHikariPoolMXBean() != null
            ? hikari.getHikariPoolMXBean().getIdleConnections()
            : "N/A");
        hikariInfo.put("totalConnections", hikari.getHikariPoolMXBean() != null
            ? hikari.getHikariPoolMXBean().getTotalConnections()
            : "N/A");
        result.put("hikari", hikariInfo);
      } else {
        result.put("datasourceType", primaryDataSource.getClass().getName());
        result.put("warning", "Datasource n'est pas une instance HikariDataSource");
      }

      return ResponseEntity.ok(result);
    } catch (Exception e) {
      log.error("Erreur lors du test de connexion à la base de données", e);
      result.put("status", "DOWN");
      result.put("error", e.getMessage());
      return ResponseEntity.status(503).body(result);
    }
  }
}

