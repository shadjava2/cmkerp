package cd.shad.erp.cmk.cmkerp.config;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.API_V1;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.API_V2;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration Swagger/OpenAPI pour le module gateway.
 *
 * <p>Cette configuration complète {@link cd.shad.erp.cmk.cmkerp.platform.config.ApiDocumentationConfig}
 * en définissant les groupes d'API versionnés et les packages à scanner.
 *
 * <p>Configuration :
 * <ul>
 *   <li>Scan de tous les contrôleurs dans le package cd.shad.erp.cmk.cmkerp</li>
 *   <li>Groupes d'API versionnés (v1 actif, v2 préparé pour le futur)</li>
 *   <li>Compatible avec les contrôleurs du platform et du gateway</li>
 * </ul>
 *
 * <p>Les contrôleurs REST sont maintenant dans leurs modules respectifs :
 * <ul>
 *   <li>cd.shad.erp.cmk.cmkerp.gateway.restcontroller (gateway - auth, health, debug)</li>
 *   <li>cd.shad.erp.cmk.cmkerp.gateway.security.web (gateway - sécurité)</li>
 *   <li>cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller (module stocks)</li>
 *   <li>cd.shad.erp.cmk.cmkerp.platform.*.application.restcontroller (module platform)</li>
 * </ul>
 *
 * <p><strong>Version actuelle :</strong> v1 (stable pour front-end Next.js)
 * <br>
 * <strong>Version future :</strong> v2 (préparée pour évolution sans casser v1)
 *

 */
@Configuration
public class SwaggerConfig {

    /**
     * Configuration du groupe d'API v1 (version stable actuelle).
     *
     * <p>Ce groupe expose uniquement les endpoints sous {@code /api/v1/**}.
     * C'est la version utilisée par le front-end Next.js et tous les clients externes.
     *
 * <p>Ce groupe scanne tous les contrôleurs REST dans le package racine
 * cd.shad.erp.cmk.cmkerp, incluant :
 * <ul>
 *   <li>Les contrôleurs du gateway (cd.shad.erp.cmk.cmkerp.gateway.restcontroller)</li>
 *   <li>Les contrôleurs de sécurité (cd.shad.erp.cmk.cmkerp.gateway.security.web)</li>
 *   <li>Les contrôleurs du module stocks (cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller)</li>
 *   <li>Les contrôleurs du module platform (cd.shad.erp.cmk.cmkerp.platform.*.application.restcontroller)</li>
 * </ul>
     *
     * @return la configuration du groupe d'API v1
     */
    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("api-v1")
                .displayName("CMK ERP – API v1 (stable pour front-end)")
                .pathsToMatch(API_V1 + "/**")
                .packagesToScan("cd.shad.erp.cmk.cmkerp")
                .build();
    }

    /**
     * Configuration du groupe d'API v2 (préparé pour le futur).
     *
     * <p>Ce groupe est préparé pour la future version de l'API sous {@code /api/v2/**}.
     * Actuellement désactivé (aucun endpoint), mais prêt pour migration progressive.
     *
     * <p><strong>Note :</strong> Quand v2 sera activée, on pourra maintenir v1 en parallèle
     * pour assurer la compatibilité avec les clients existants.
     *
     * @return la configuration du groupe d'API v2 (préparé)
     */
    @Bean
    public GroupedOpenApi apiV2() {
        return GroupedOpenApi.builder()
                .group("api-v2")
                .displayName("CMK ERP – API v2 (future)")
                .pathsToMatch(API_V2 + "/**")
                .packagesToScan("cd.shad.erp.cmk.cmkerp")
                .build();
    }

    /**
     * Configuration du groupe d'API par défaut (utilise v1).
     *
     * <p>Ce groupe est utilisé quand on accède à /v3/api-docs sans spécifier de groupe.
     * Il expose uniquement l'API v1 pour une vue claire et stable.
     *
     * @return la configuration du groupe d'API par défaut (v1)
     */
    @Bean
    public GroupedOpenApi defaultApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .displayName("CMK ERP API v1 - Documentation complète")
                .pathsToMatch(API_V1 + "/**")
                .packagesToScan("cd.shad.erp.cmk.cmkerp")
                .build();
    }

}

