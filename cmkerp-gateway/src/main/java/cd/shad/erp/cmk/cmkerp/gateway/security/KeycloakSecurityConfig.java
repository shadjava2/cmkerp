package cd.shad.erp.cmk.cmkerp.gateway.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import lombok.RequiredArgsConstructor;

/**
 * Configuration Spring Security avec Keycloak (IAM entreprise).
 *
 * <p>
 * Keycloak offre une gestion centralisée des utilisateurs, SSO, MFA, et une intégration OAuth2/OIDC
 * complète.
 *
 * <p>
 * Avantages :
 * <ul>
 * <li>IAM centralisé (gestion utilisateurs, rôles, permissions)</li>
 * <li>SSO (Single Sign-On) multi-applications</li>
 * <li>MFA (Multi-Factor Authentication)</li>
 * <li>OAuth2/OIDC standard</li>
 * <li>Gestion des sessions distribuées</li>
 * </ul>
 *
 * <p>
 * Configuration requise dans application.yml :
 *
 * <pre>{@code
 * spring:
 *   security:
 *     oauth2:
 *       resourceserver:
 *         jwt:
 *           issuer-uri: https://keycloak.example.com/realms/cmkerp
 * keycloak:
 *   realm: cmkerp
 *   auth-server-url: https://keycloak.example.com
 *   resource: cmkerp-backend
 *   credentials:
 *     secret: ${KEYCLOAK_CLIENT_SECRET}
 * }</pre>
 *
 * <p>
 * <strong>Note:</strong> Cette configuration est conditionnelle. Si Keycloak n'est pas activé, la
 * configuration JWT standard est utilisée.
 *
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class KeycloakSecurityConfig {

  /**
   * Configuration de la chaîne de filtres de sécurité avec Keycloak.
   *
   * <p>
   * Utilise OAuth2 Resource Server avec Keycloak comme issuer. Les tokens JWT sont validés
   * automatiquement via l'endpoint JWK Set de Keycloak.
   *
   * @param http HttpSecurity
   * @return SecurityFilterChain configurée
   * @throws Exception en cas d'erreur de configuration
   */
  @Bean
  public SecurityFilterChain keycloakSecurityFilterChain(HttpSecurity http) throws Exception {
    http
        // Désactiver CSRF pour API stateless
        .csrf(csrf -> csrf.disable())
        // Session stateless
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        // Configuration CORS
        .cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // Configuration OAuth2 Resource Server (Keycloak)
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {
          // La configuration issuer-uri est automatiquement découverte depuis application.yml
          // spring.security.oauth2.resourceserver.jwt.issuer-uri
        }))
        // Configuration des autorisations
        .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/auth/**").permitAll()
            .requestMatchers("/api/v1/health").permitAll().requestMatchers("/actuator/health")
            .permitAll().requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
            .anyRequest().authenticated());

    return http.build();
  }

  /**
   * Configuration CORS.
   *
   * @return CorsConfigurationSource
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.addAllowedOrigin("http://localhost:3000");
    configuration.addAllowedOrigin("http://127.0.0.1:3000");
    configuration.addAllowedOrigin("http://localhost:3940");
    configuration.addAllowedOrigin("https://cmkerp.com");
    configuration.addAllowedOrigin("https://www.cmkerp.com");
    configuration.addAllowedMethod("GET");
    configuration.addAllowedMethod("POST");
    configuration.addAllowedMethod("PUT");
    configuration.addAllowedMethod("PATCH");
    configuration.addAllowedMethod("DELETE");
    configuration.addAllowedMethod("OPTIONS");
    configuration.addAllowedHeader("Authorization");
    configuration.addAllowedHeader("Content-Type");
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}

