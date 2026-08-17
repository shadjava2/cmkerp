package cd.shad.erp.cmk.cmkerp.platform.config.db;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.zaxxer.hikari.HikariConfig;

/**
 * Propriétés de configuration pour la datasource primaire.
 *
 * <p>
 * Mappe les propriétés avec le préfixe {@code cmk.datasource.primary.*} vers
 * une instance de {@link HikariConfig}.
 *
 * <p>
 * Configuration dans application-dev.yml :
 *
 * <pre>{@code
 * cmk:
 *   datasource:
 *     primary:
 *       url: jdbc:mysql://localhost:3306/cmk_erp
 *       username: cmk_user
 *       password: cmk_pass
 *       driver-class-name: com.mysql.cj.jdbc.Driver
 *       maximum-pool-size: 10
 *       pool-name: CMK-ERP-PrimaryPool
 * }</pre>
 *
 * <p>
 * Note: Le setter {@code setUrl()} mappe automatiquement vers {@code setJdbcUrl()}
 * pour compatibilité avec HikariCP qui attend {@code jdbcUrl}.
 */
@ConfigurationProperties(prefix = "cmk.datasource.primary")
public class PrimaryDataSourceProperties extends HikariConfig {

    /**
     * Setter pour mapper 'url' (YAML) vers 'jdbcUrl' (HikariCP).
     *
     * @param url l'URL JDBC depuis la configuration YAML
     */
    public void setUrl(String url) {
        setJdbcUrl(url);
    }

    /**
     * Getter pour compatibilité avec les propriétés YAML.
     *
     * @return l'URL JDBC
     */
    public String getUrl() {
        return getJdbcUrl();
    }

    /**
     * Setter pour mapper 'driver-class-name' (YAML) vers 'driverClassName' (HikariCP).
     *
     * @param driverClassName le nom de la classe du driver depuis la configuration YAML
     */
    public void setDriverClassName(String driverClassName) {
        super.setDriverClassName(driverClassName);
    }
}

