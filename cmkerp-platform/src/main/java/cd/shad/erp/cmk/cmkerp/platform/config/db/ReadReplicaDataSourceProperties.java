package cd.shad.erp.cmk.cmkerp.platform.config.db;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.zaxxer.hikari.HikariConfig;

/**
 * Propriétés de configuration pour le read replica MySQL.
 *
 * <p>
 * Cette classe mappe les propriétés avec le préfixe {@code cmkerp.db.read-replica.*}
 * vers une instance de {@link HikariConfig}.
 *
 * <p>
 * Configuration dans application-*.yml (désactivé par défaut) :
 * <pre>{@code
 * cmkerp:
 *   db:
 *     read-replica:
 *       enabled: true  # Activer pour utiliser les read replicas
 *       url: jdbc:mysql://read-replica-host:3306/cmkerp
 *       username: read_user
 *       password: read_pass
 *       driver-class-name: com.mysql.cj.jdbc.Driver
 *       pool-name: CMK-ERP-ReadReplicaPool
 * }</pre>
 *
 * <p>
 * Note: Le setter {@code setUrl()} mappe automatiquement vers {@code setJdbcUrl()}
 * pour compatibilité avec HikariCP qui attend {@code jdbcUrl}.
 *

 */
@ConfigurationProperties(prefix = "cmkerp.db.read-replica")
public class ReadReplicaDataSourceProperties extends HikariConfig {

    /**
     * Flag pour activer/désactiver l'utilisation des read replicas.
     * Par défaut : false (désactivé).
     */
    private boolean enabled = false;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}

