package cd.shad.erp.cmk.cmkerp.platform.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Configuration JPA explicite pour utiliser la datasource primaire.
 *
 * <p>Cette configuration permet à Spring Boot de créer l'EntityManagerFactory
 * en utilisant la datasource primaire configurée dans PrimaryDataSourceConfig.
 *
 * <p>Pourquoi cette configuration est nécessaire :
 * <ul>
 *   <li>La datasource primaire est nommée "primaryDataSource" (pas "dataSource")</li>
 *   <li>Spring Boot auto-configuration JPA cherche "dataSource" par défaut</li>
 *   <li>Cette configuration explicite utilise la datasource @Primary</li>
 * </ul>
 */
@Configuration
@EnableTransactionManagement
@EnableConfigurationProperties(JpaProperties.class)
@EnableJpaRepositories(
    basePackages = {
        "cd.shad.erp.cmk.cmkerp.platform.site.domain.repository",
        "cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.repository",
        "cd.shad.erp.cmk.cmkerp.platform.notification.domain.repository",
        "cd.shad.erp.cmk.cmkerp.platform.security.domain.repository",
        "cd.shad.erp.cmk.cmkerp.stocks.domain.repository"
    },
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
@RequiredArgsConstructor
@Slf4j
public class JpaConfig {

    private final DataSource primaryDataSource; // Injection de la datasource @Primary
    private final JpaProperties jpaProperties;

    /**
     * Crée l'EntityManagerFactory pour JPA.
     *
     * <p>Utilise la datasource primaire et les propriétés JPA configurées
     * dans application.yml (spring.jpa.*).
     *
     * @return LocalContainerEntityManagerFactoryBean configuré
     */
    @Bean(name = "entityManagerFactory")
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
        log.info("Configuration de l'EntityManagerFactory JPA avec la datasource primaire");

        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(primaryDataSource);
        em.setPackagesToScan(
            "cd.shad.erp.cmk.cmkerp.platform.site.domain.model",
            "cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model",
            "cd.shad.erp.cmk.cmkerp.platform.notification.domain.model",
            "cd.shad.erp.cmk.cmkerp.platform.security.domain.model",
            "cd.shad.erp.cmk.cmkerp.stocks.domain.model"
        );

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);
        em.setJpaPropertyMap(jpaProperties.getProperties());

        return em;
    }

    /**
     * Crée le TransactionManager pour JPA.
     *
     * <p>Ce TransactionManager gère les transactions pour :
     * <ul>
     *   <li>Les repositories JPA (via EntityManager)</li>
     *   <li>Les opérations JDBC directes (JdbcTemplate, NamedParameterJdbcTemplate)</li>
     * </ul>
     *
     * <p>Le JpaTransactionManager peut gérer les deux types d'opérations sur la même datasource,
     * ce qui permet d'avoir un seul transaction manager pour toute l'application.
     *
     * @param emf l'EntityManagerFactory
     * @return PlatformTransactionManager configuré
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
        log.info("Configuration du TransactionManager JPA (gère JPA et JDBC)");
        JpaTransactionManager transactionManager = new JpaTransactionManager(emf);

        // Optimisation : valider les transactions existantes pour les opérations en lecture seule
        // Cela permet de détecter les problèmes de configuration plus tôt
        transactionManager.setValidateExistingTransaction(true);

        return transactionManager;
    }
}

