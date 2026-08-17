package cd.shad.erp.cmk.cmkerp.platform.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
@ConditionalOnClass(name = "io.swagger.v3.oas.models.OpenAPI")
public class ApiDocumentationConfig {
  @Bean
  public OpenAPI cmkErpPlatformOpenAPI() {
    String description = """
        API CMK ERP Platform

        Architecture modulaire :
        - cmkerp-gateway : Point d'entrée API / Auth
        - cmkerp-platform : Services métier
        - cmkerp-shared-kernel : Modèles partagés
        - cmkerp-bom : Gestion des dépendances

        Authentification JWT requise pour les endpoints protégés.
        """;

    return new OpenAPI()
        .info(new Info().title("CMK ERP Platform API").description(description).version("4.1.1")
            .contact(new Contact().name("Schadrack MPAKA MABI").email("mpakadev@cmk-cd.org")
                .url("https://cmkerp.com")
                .extensions(java.util.Map.of("phone", "+243 817 028 987")))
            .license(new License().name("Proprietary").url("https://cmkerp.com/license")))
        .addServersItem(new Server().url("http://localhost:8984/cmkerp-gateway")
            .description("Serveur de développement"))
        .addServersItem(new Server().url("https://api.cmkerp.com/cmkerp-gateway")
            .description("Serveur de production"))
        .addTagsItem(new Tag().name("Platform - Utilisateurs")
            .description("Gestion des utilisateurs, rôles, permissions"))
        .addTagsItem(new Tag().name("Platform - Pharmacies")
            .description("Gestion pharmacie, droits, structures liées"))
        .addTagsItem(new Tag().name("Platform - Sites")
            .description("Gestion des sites et structures organisationnelles"))
        .addTagsItem(new Tag().name("Platform - Notifications")
            .description("Envoi, états & tracking des notifications internes"))
        .addTagsItem(
            new Tag().name("Platform - Dashboard").description("Tableaux de bord et statistiques"))
        .addTagsItem(new Tag().name("Core - Shared Kernel")
            .description("Objets transversaux & base universelle du domaine"))
        .addTagsItem(new Tag().name("Gateway - Authentification")
            .description("Authentification, sécurité & entrée API"))
        .components(new Components().addSecuritySchemes("bearerAuth",
            new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")
                .description("Authentification JWT. Format: Bearer <token>")))
        .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
  }
}
