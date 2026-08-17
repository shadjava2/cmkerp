package cd.shad.erp.cmk.cmkerp.gateway.security;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.AUTH_PATTERN;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.HEALTH_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.PORTAIL_FOURNISSEUR_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCK_INTELLIGENCE_WHATSAPP_WEBHOOK;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import lombok.RequiredArgsConstructor;

/**
 * Configuration Spring Security pour le module gateway.
 *
 * <p>
 * Configure :
 * <ul>
 * <li>Désactivation de CSRF (API stateless avec JWT)</li>
 * <li>Session stateless</li>
 * <li>CORS activé</li>
 * <li>Filtre JWT pour l'authentification</li>
 * <li>Autorisations : /api/v1/auth/** et /api/v1/health en permitAll, reste authentifié</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
// ✅ SECURITY SCOPE REDUCTION: @EnableMethodSecurity désactivé - AUTH uniquement (pas de RBAC/ACL)
// @EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  @Value("${platform.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,http://localhost:3838,http://127.0.0.1:3838,http://localhost:3940,http://127.0.0.1:3940,https://cmkerp.com,https://www.cmkerp.com,http://cmkerp.com,http://www.cmkerp.com}")
  private String allowedOrigins;

  @Value("${platform.cors.allow-credentials:true}")
  private boolean allowCredentials;

  private final JwtAuthenticationFilter jwtAuthenticationFilter;

  /**
   * Configuration de la chaîne de filtres de sécurité.
   *
   * <p>
   * Architecture actuelle :
   * <ul>
   * <li>JWT stateless avec filtre personnalisé (JwtAuthenticationFilter)</li>
   * <li>Compatible avec OAuth2/OIDC Resource Server (prêt pour Keycloak)</li>
   * <li>WebSocket sécurisé avec authentification obligatoire</li>
   * <li>Rate limiting activé via RateLimitFilter</li>
   * </ul>
   *
   * <p>
   * Pour activer OAuth2/OIDC avec un IdP externe (Keycloak) :
   * <ol>
   * <li>Configurer dans application.yml :
   *
   * <pre>{@code
   * spring:
   *   security:
   *     oauth2:
   *       resourceserver:
   *         jwt:
   *           issuer-uri: https://keycloak.example.com/realms/cmkerp
   * }</pre>
   *
   * </li>
   * <li>Optionnel : remplacer JwtAuthenticationFilter par OAuth2ResourceServerJwtConfigurer dans
   * cette méthode</li>
   * </ol>
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        // Désactiver CSRF pour API stateless
        .csrf(AbstractHttpConfigurer::disable)
        // Désactiver HTML login form (API REST pure)
        .formLogin(AbstractHttpConfigurer::disable)
        // Désactiver HTTP Basic (on utilise JWT uniquement)
        .httpBasic(AbstractHttpConfigurer::disable)
        // Session stateless (pas de session HTTP)
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Configuration CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // Configuration exception handling : retourner HTTP status au lieu de redirections
        .exceptionHandling(ex -> ex
            // Retourner 401 (UNAUTHORIZED) au lieu de rediriger vers /login
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            // Retourner 403 (FORBIDDEN) pour les accès refusés
            .accessDeniedHandler((request, response, accessDeniedException) -> {
              response.setStatus(HttpStatus.FORBIDDEN.value());
              response.setContentType("application/json");
              response.getWriter()
                  .write("{\"error\":\"Forbidden\",\"message\":\"Access denied\",\"status\":403}");
            }))
        // Configuration des autorisations
        .authorizeHttpRequests(auth -> auth
            // Endpoints publics (API v1)
            .requestMatchers(AUTH_PATTERN).permitAll().requestMatchers(HEALTH_BASE).permitAll() // /api/v1/health
            .requestMatchers(STOCK_INTELLIGENCE_WHATSAPP_WEBHOOK, STOCK_INTELLIGENCE_WHATSAPP_WEBHOOK + "/**").permitAll()
            .requestMatchers(PORTAIL_FOURNISSEUR_BASE, PORTAIL_FOURNISSEUR_BASE + "/**").permitAll()
            .requestMatchers("/actuator/health").permitAll().requestMatchers("/actuator/info")
            .permitAll().requestMatchers("/debug/**").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
            // ✅ SUPPRIMÉ : WebSocket complètement supprimé
            // .requestMatchers("/ws/**").authenticated()
            // Endpoints Actuator restants nécessitent une authentification en prod
            // (metrics, prometheus, etc.)
            .requestMatchers("/actuator/**").authenticated()
            // Tout le reste nécessite une authentification
            .anyRequest().authenticated())
        // Ajouter le filtre JWT avant UsernamePasswordAuthenticationFilter
        // Note: Le RateLimitFilter est ajouté via @Order(1) dans la classe du filtre
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  /**
   * Configuration CORS pour permettre les requêtes cross-origin.
   *
   * <p>
   * Configuration :
   * <ul>
   * <li>Origines autorisées : localhost:3000, 127.0.0.1:3000</li>
   * <li>Méthodes : GET, POST, PUT, DELETE, OPTIONS</li>
   * <li>Headers : Authorization, Content-Type</li>
   * <li>Credentials : autorisés</li>
   * </ul>
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    List<String> origins = Arrays.stream(allowedOrigins.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
    if ("*".equals(allowedOrigins.trim()) && allowCredentials) {
      configuration.setAllowedOriginPatterns(List.of("*"));
    } else {
      configuration.setAllowedOrigins(origins);
    }
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(allowCredentials);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  /**
   * Bean AuthenticationManager pour l'authentification.
   */
  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }

  // Note: Le bean PasswordEncoder est défini dans SecurityBeansConfig (platform)
  // pour éviter les duplications. Il est accessible via injection de dépendance.
}

