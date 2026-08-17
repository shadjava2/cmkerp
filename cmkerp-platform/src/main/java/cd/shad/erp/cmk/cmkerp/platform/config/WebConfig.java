package cd.shad.erp.cmk.cmkerp.platform.config;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration optimisée Web MVC pour le module platform.
 *
 * <p>
 * Configure les aspects suivants pour les contrôleurs REST :
 * <ul>
 * <li><strong>CORS</strong> : Cross-Origin Resource Sharing sécurisé pour les API REST</li>
 * <li><strong>Formatters</strong> : support des types temporels (LocalDateTime, etc.) aligné avec
 * JacksonConfig</li>
 * <li><strong>Content Negotiation</strong> : négociation de contenu JSON par défaut</li>
 * <li><strong>Message Converters</strong> : optimisation des convertisseurs HTTP</li>
 * </ul>
 *
 * <p>
 * Cette configuration s'applique uniquement aux contrôleurs REST. Pas de configuration de vues
 * (Thymeleaf, JSP) car l'application est une API pure.
 *
 * <p>
 * <strong>Alignement avec les autres configurations :</strong>
 * <ul>
 * <li>Format de date aligné avec {@link JacksonConfig#DATE_TIME_FORMATTER} :
 * {@code yyyy-MM-dd'T'HH:mm:ss}</li>
 * <li>Style de documentation cohérent avec {@link TransactionConfig} et
 * {@link SecurityBeansConfig}</li>
 * <li>Gestion des propriétés configurables via application.properties</li>
 * </ul>
 *
 * <p>
 * Configuration des propriétés (application.properties) :
 *
 * <pre>
 * {@code
 * # CORS Configuration
 * platform.cors.allowed-origins=http://localhost:3000,http://localhost:8080
 * platform.cors.allowed-methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
 * platform.cors.allowed-headers=Content-Type,Authorization,X-Requested-With,Accept,Origin
 * platform.cors.max-age=3600
 * platform.cors.allow-credentials=true
 *
 * # Content Negotiation
 * platform.web.default-content-type=application/json
 * }
 * </pre>
 *
 * <p>
 * <strong>Sécurité CORS :</strong>
 * <ul>
 * <li>En développement : utilisez des origines spécifiques (ex: http://localhost:3000)</li>
 * <li>En production : <strong>NE JAMAIS</strong> utiliser "*" pour allowedOrigins si
 * allowCredentials=true</li>
 * <li>Resserrer les origines autorisées selon les besoins réels de l'application</li>
 * </ul>
 *

 * @see JacksonConfig
 * @see TransactionConfig
 * @see SecurityBeansConfig
 */
@Configuration
@ConditionalOnClass(name = "org.springframework.web.servlet.config.annotation.WebMvcConfigurer")
public class WebConfig implements WebMvcConfigurer {

  private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

  /**
   * Origines autorisées pour CORS (séparées par des virgules). Peut être configuré via
   * application.properties : platform.cors.allowed-origins
   *
   * <p>
   * Valeur par défaut : "http://localhost:3000,http://127.0.0.1:3000" (origines de développement).
   *
   * <p>
   * <strong>IMPORTANT PRODUCTION :</strong> Ne jamais utiliser "*" si allowCredentials=true.
   * Utiliser des origines spécifiques :
   * <ul>
   * <li>Développement : "http://localhost:3000,http://localhost:8080"</li>
   * <li>Production : "https://app.cmkerp.cd,https://admin.cmkerp.cd"</li>
   * </ul>
   */
  @Value("${platform.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://localhost:3838,http://127.0.0.1:3838,http://localhost:3940,http://127.0.0.1:3940,https://cmkerp.com,https://www.cmkerp.com,http://cmkerp.com,http://www.cmkerp.com}")
  private String allowedOrigins;

  /**
   * Méthodes HTTP autorisées pour CORS. Peut être configuré via application.properties :
   * platform.cors.allowed-methods
   */
  @Value("${platform.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
  private String allowedMethods;

  /**
   * Headers autorisés pour CORS. Peut être configuré via application.properties :
   * platform.cors.allowed-headers
   *
   * <p>
   * IMPORTANT : Inclut "Cookie" pour permettre l'envoi des cookies HttpOnly.
   */
  @Value("${platform.cors.allowed-headers:Content-Type,Authorization,X-Requested-With,Accept,Origin,Cookie}")
  private String allowedHeaders;

  /**
   * Durée de mise en cache des prérequêtes CORS (en secondes). Peut être configuré via
   * application.properties : platform.cors.max-age
   */
  @Value("${platform.cors.max-age:3600}")
  private long maxAge;

  /**
   * Autorise l'envoi de credentials (cookies, headers d'authentification) dans les requêtes CORS.
   * Peut être configuré via application.properties : platform.cors.allow-credentials
   *
   * <p>
   * <strong>IMPORTANT :</strong> Si true, allowedOrigins ne doit PAS contenir "*" (wildcard).
   */
  @Value("${platform.cors.allow-credentials:true}")
  private boolean allowCredentials;

  /**
   * Type de contenu par défaut pour les réponses HTTP. Peut être configuré via
   * application.properties : platform.web.default-content-type
   */
  @Value("${platform.web.default-content-type:application/json}")
  private String defaultContentType;

  /**
   * Configure les mappings CORS pour les endpoints REST.
   *
   * <p>
   * Configuration appliquée :
   * <ul>
   * <li>Endpoints API v1 : {@code /api/v1/**} - CORS complet avec credentials</li>
   * <li>Endpoints API v2 : {@code /api/v2/**} - CORS complet avec credentials (pour le futur)</li>
   * <li>Endpoints racine : {@code /**} - CORS limité (GET, OPTIONS uniquement) pour les ressources
   * statiques</li>
   * </ul>
   *
   * <p>
   * Gestion du wildcard "*" : si allowedOrigins contient "*", utilise allowedOriginPatterns à la
   * place pour éviter les conflits avec allowCredentials.
   */
  @Override
  public void addCorsMappings(CorsRegistry registry) {
    log.info("Configuration CORS - allowedOrigins: {}, allowCredentials: {}", allowedOrigins,
        allowCredentials);

    // Configuration pour les endpoints API v1 (version actuelle stable)
    var apiV1Cors = registry.addMapping("/api/v1/**").allowedMethods(allowedMethods.split(","))
        .allowedHeaders(allowedHeaders.split(",")).allowCredentials(allowCredentials)
        .maxAge(maxAge);

    // Configuration pour les endpoints API v2 (préparé pour le futur)
    var apiV2Cors = registry.addMapping("/api/v2/**").allowedMethods(allowedMethods.split(","))
        .allowedHeaders(allowedHeaders.split(",")).allowCredentials(allowCredentials)
        .maxAge(maxAge);

    // Gestion du wildcard "*" : utiliser allowedOriginPatterns si "*" est présent
    if ("*".equals(allowedOrigins.trim())) {
      if (allowCredentials) {
        log.warn("CORS: allowedOrigins='*' avec allowCredentials=true est incompatible. "
            + "Utilisation de allowedOriginPatterns='*' (moins sécurisé). "
            + "En production, spécifiez des origines explicites.");
        apiV1Cors.allowedOriginPatterns("*");
        apiV2Cors.allowedOriginPatterns("*");
      } else {
        apiV1Cors.allowedOrigins("*");
        apiV2Cors.allowedOrigins("*");
      }
    } else {
      List<String> origins = Arrays.asList(allowedOrigins.split(","));
      apiV1Cors.allowedOrigins(origins.toArray(new String[0]));
      apiV2Cors.allowedOrigins(origins.toArray(new String[0]));
      log.info("CORS configuré pour API v1 et v2 avec {} origine(s) spécifique(s)", origins.size());
    }

    // Configuration pour les endpoints racine (ressources statiques, health checks, etc.)
    var rootCors = registry.addMapping("/**").allowedMethods("GET", "OPTIONS").maxAge(maxAge);

    if ("*".equals(allowedOrigins.trim())) {
      if (allowCredentials) {
        rootCors.allowedOriginPatterns("*");
      } else {
        rootCors.allowedOrigins("*");
      }
    } else {
      List<String> origins = Arrays.asList(allowedOrigins.split(","));
      rootCors.allowedOrigins(origins.toArray(new String[0]));
    }
  }

  /**
   * Configure les formatters pour les types temporels Java (LocalDateTime, LocalDate, etc.).
   *
   * <p>
   * Format utilisé : {@code yyyy-MM-dd'T'HH:mm:ss} (aligné avec
   * {@link JacksonConfig#DATE_TIME_FORMATTER}).
   *
   * <p>
   * Ce format est utilisé pour :
   * <ul>
   * <li>Les paramètres de requête (ex: {@code ?date=2024-01-15T10:30:00})</li>
   * <li>Les variables de chemin (ex: {@code /api/events/{date}})</li>
   * <li>Les conversions dans les contrôleurs REST</li>
   * </ul>
   */
  @Override
  public void addFormatters(FormatterRegistry registry) {
    log.info(
        "Configuration des formatters pour les types temporels - format: yyyy-MM-dd'T'HH:mm:ss");

    DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();

    // Utilise le format ISO-8601 par défaut pour la compatibilité
    registrar.setUseIsoFormat(true);

    // Format personnalisé aligné avec JacksonConfig pour la cohérence
    registrar.setDateTimeFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
    registrar.setDateFormatter(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    registrar.setTimeFormatter(DateTimeFormatter.ofPattern("HH:mm:ss"));

    registrar.registerFormatters(registry);
  }

  /**
   * Configure la négociation de contenu pour les réponses HTTP.
   *
   * <p>
   * Configuration appliquée :
   * <ul>
   * <li>Type de contenu par défaut : JSON (application/json)</li>
   * <li>Désactivation des paramètres de format dans l'URL (ex: ?format=json)</li>
   * <li>Priorité au header Accept pour la négociation</li>
   * </ul>
   */
  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    log.info("Configuration de la négociation de contenu - defaultContentType: {}",
        defaultContentType);

    configurer.defaultContentType(MediaType.valueOf(defaultContentType)).favorParameter(false) // Désactive
                                                                                               // ?format=json
        .ignoreAcceptHeader(false); // Respecte le header Accept
  }

  /**
   * Configure les message converters HTTP pour optimiser la sérialisation/désérialisation.
   *
   * <p>
   * Optimisations appliquées :
   * <ul>
   * <li>Les converters Jackson sont déjà configurés par Spring Boot avec les paramètres de
   * JacksonConfig</li>
   * <li>Support des types Java Time via Jackson (aligné avec JacksonConfig)</li>
   * </ul>
   *
   * <p>
   * Note: Les converters par défaut de Spring Boot sont suffisants. Cette méthode peut être
   * utilisée pour ajouter des converters personnalisés si nécessaire. Les converters Jackson sont
   * automatiquement configurés via JacksonConfig.
   */
  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    // Les converters Jackson sont déjà configurés par Spring Boot avec les paramètres de
    // JacksonConfig. Aucune configuration supplémentaire nécessaire.
    log.debug("Message converters configurés - {} converters disponibles (incluant Jackson)",
        converters.size());
  }
}
