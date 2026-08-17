package cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_REPORTS_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_BASE;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitWithStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ProduitQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;

/**
 * Contrôleur REST pour la génération de rapports.
 * Expose les endpoints pour générer des rapports PDF via JasperReports.
 */
@RestController
@RequestMapping({STOCKS_BASE + "/reports", POS_REPORTS_BASE})
@RequiredArgsConstructor
@Tag(name = "Stocks - Rapports", description = "Génération de rapports PDF")
@Slf4j
public class ReportRestController {

    private final ReportService reportService;
    private final ProduitQueryService produitQueryService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Génère un rapport PDF de la liste des produits.
     *
     * @param pharmacieId ID de la pharmacie pour filtrer les produits (requis)
     * @param pharmacieNom Nom de la pharmacie pour l'en-tête du rapport
     * @param nomcommercial Recherche optionnelle par nom commercial
     * @param operationnel Filtre optionnel sur le statut opérationnel
     * @return PDF en streaming avec Content-Disposition: attachment
     */
    @GetMapping("/produits")
    @Operation(summary = "Génère un rapport PDF de la liste des produits (page actuelle uniquement)")
    public ResponseEntity<byte[]> generateProduitsReport(
            @RequestParam(required = false) Long pharmacieId,
            @RequestParam(required = false) String pharmacieNom,
            @RequestParam(required = false) String nomcommercial,
            @RequestParam(required = false) Boolean operationnel,
            // Filtres avancés
            @RequestParam(required = false) Boolean perime,
            @RequestParam(required = false) Integer perimeDansXJours,
            @RequestParam(required = false) String stockOperator,
            @RequestParam(required = false) Double stockValue,
            @RequestParam(required = false) String prixOperator,
            @RequestParam(required = false) Double prixValue,
            @RequestParam(required = false) Boolean perimable,
            // 🎯 Pagination pour générer le rapport uniquement pour la page actuelle
            // CRITICAL: required = true pour forcer la transmission des paramètres (évite les valeurs par défaut)
            @RequestParam(required = true) Integer page,
            @RequestParam(required = true) Integer size,
            HttpServletRequest request) {

        log.info("🚀 [ReportRestController] Début génération rapport produits - pharmacieId: {}, pharmacieNom: {}, nomcommercial: {}, operationnel: {}",
                pharmacieId, pharmacieNom, nomcommercial, operationnel);
        log.debug("🚀 [ReportRestController] Filtres avancés - perime: {}, perimeDansXJours: {}, stockOperator: {}, stockValue: {}, prixOperator: {}, prixValue: {}, perimable: {}",
                perime, perimeDansXJours, stockOperator, stockValue, prixOperator, prixValue, perimable);

        try {
            // Validation: pharmacieId est requis
            if (pharmacieId == null) {
                log.warn("❌ [ReportRestController] pharmacieId est null - retour BAD_REQUEST");
                return ResponseEntity.badRequest().build();
            }

            log.info("📊 [ReportRestController] Récupération des produits avec filtres...");
            // 🎯 OPTIMISATION: Utiliser findProductsWithStockPage avec la pagination actuelle
            // Récupérer uniquement les produits de la page actuelle (pas tous les produits)
            // Appliquer les mêmes filtres que la liste pour cohérence parfaite
            org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
            log.info("📄 [ReportRestController] Génération rapport pour page={}, size={}", page, size);
            PageResponse<ProduitWithStockResponse> pageResponse = produitQueryService
                    .findProductsWithStockPage(
                            pharmacieId,
                            nomcommercial,
                            operationnel,
                            pageable,
                            perime,
                            perimeDansXJours,
                            stockOperator,
                            stockValue,
                            prixOperator,
                            prixValue,
                            perimable
                    );
            List<ProduitWithStockResponse> produits = pageResponse.getContent();

            log.info("✅ [ReportRestController] {} produits récupérés (page {}), génération du PDF...", produits.size(), page);

            // Récupérer le nom de l'utilisateur connecté
            String utilisateurNom = getCurrentUsername(request);
            log.debug("[ReportRestController] Utilisateur: {}", utilisateurNom);

            // Générer le rapport PDF
            byte[] pdfBytes = reportService.generateProduitsReport(produits, pharmacieNom, utilisateurNom);

            log.info("✅ [ReportRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline dans un iframe avec optimisations
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "rapport-produits-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);
            // Optimisations de performance
            headers.setCacheControl("no-cache, no-store, must-revalidate"); // Pas de cache pour les rapports dynamiques
            headers.setPragma("no-cache");
            headers.setExpires(0);
            // Compression Gzip activée automatiquement par Spring Boot si configurée
            // La compression est gérée par le serveur (Tomcat/Undertow) selon la configuration

            log.info("✅ [ReportRestController] Rapport produits généré avec succès: {} produits, taille: {} bytes, filename: {}",
                    produits.size(), pdfBytes.length, filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport produits: {}", e.getMessage(), e);
            return buildErrorResponse("Erreur JasperReports: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération du rapport produits: {}", e.getMessage(), e);
            return buildErrorResponse("Erreur: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport: {}", e.getMessage(), e);
            return buildErrorResponse("Erreur inattendue: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Génère un rapport Excel de la liste des produits.
     *
     * @param pharmacieId ID de la pharmacie pour filtrer les produits (requis)
     * @param pharmacieNom Nom de la pharmacie pour l'en-tête du rapport
     * @param nomcommercial Recherche optionnelle par nom commercial
     * @param operationnel Filtre optionnel sur le statut opérationnel
     * @return Excel en streaming avec Content-Disposition: attachment
     */
    @GetMapping("/produits/excel")
    @Operation(summary = "Génère un rapport Excel de la liste des produits")
    public ResponseEntity<byte[]> generateProduitsReportExcel(
            @RequestParam(required = false) Long pharmacieId,
            @RequestParam(required = false) String pharmacieNom,
            @RequestParam(required = false) String nomcommercial,
            @RequestParam(required = false) Boolean operationnel,
            // Filtres avancés
            @RequestParam(required = false) Boolean perime,
            @RequestParam(required = false) Integer perimeDansXJours,
            @RequestParam(required = false) String stockOperator,
            @RequestParam(required = false) Double stockValue,
            @RequestParam(required = false) String prixOperator,
            @RequestParam(required = false) Double prixValue,
            @RequestParam(required = false) Boolean perimable) {

        log.info("🚀 [ReportRestController] Début génération rapport Excel produits - pharmacieId: {}, pharmacieNom: {}, nomcommercial: {}, operationnel: {}",
                pharmacieId, pharmacieNom, nomcommercial, operationnel);
        log.debug("🚀 [ReportRestController] Filtres avancés - perime: {}, perimeDansXJours: {}, stockOperator: {}, stockValue: {}, prixOperator: {}, prixValue: {}, perimable: {}",
                perime, perimeDansXJours, stockOperator, stockValue, prixOperator, prixValue, perimable);

        try {
            // Validation: pharmacieId est requis
            if (pharmacieId == null) {
                log.warn("❌ [ReportRestController] pharmacieId est null - retour BAD_REQUEST");
                return ResponseEntity.badRequest().build();
            }

            log.info("📊 [ReportRestController] Récupération des produits avec filtres...");
            // 🎯 OPTIMISATION: Utiliser findProductsWithStockPage pour garantir la cohérence des filtres avec la liste
            // Récupérer tous les produits (sans pagination pour le rapport)
            // On utilise une pageable avec une taille très grande pour récupérer tous les produits
            // Appliquer les mêmes filtres que la liste pour cohérence parfaite
            org.springframework.data.domain.Pageable pageable = PageRequest.of(0, 10000);
            PageResponse<ProduitWithStockResponse> pageResponse = produitQueryService
                    .findProductsWithStockPage(
                            pharmacieId,
                            nomcommercial,
                            operationnel,
                            pageable,
                            perime,
                            perimeDansXJours,
                            stockOperator,
                            stockValue,
                            prixOperator,
                            prixValue,
                            perimable
                    );
            List<ProduitWithStockResponse> produits = pageResponse.getContent();

            log.info("✅ [ReportRestController] {} produits récupérés, génération du Excel...", produits.size());

            // Générer le rapport Excel
            byte[] excelBytes = reportService.generateProduitsReportExcel(produits, pharmacieNom);

            log.info("✅ [ReportRestController] Excel généré: {} bytes", excelBytes.length);

            // Préparer les headers pour le téléchargement
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String filename = "rapport-produits-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            log.info("✅ [ReportRestController] Rapport Excel produits généré avec succès: {} produits, taille: {} bytes, filename: {}",
                    produits.size(), excelBytes.length, filename);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération du rapport Excel produits: {}", e.getMessage(), e);
            return buildErrorResponse("Erreur: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport Excel: {}", e.getMessage(), e);
            return buildErrorResponse("Erreur inattendue: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Construit une réponse d'erreur en JSON.
     */
    private ResponseEntity<byte[]> buildErrorResponse(String message, HttpStatus status) {
        try {
            Map<String, String> error = new HashMap<>();
            error.put("error", message);
            error.put("status", status.toString());

            String jsonError = "{\"error\":\"" + message.replace("\"", "\\\"") + "\",\"status\":\"" + status + "\"}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            return new ResponseEntity<>(jsonError.getBytes(), headers, status);
        } catch (Exception e) {
            log.error("Erreur lors de la construction de la réponse d'erreur", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Récupère le nom d'utilisateur depuis le JWT token.
     */
    private String getCurrentUsername(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("[ReportRestController] Utilisateur non authentifié");
            return "";
        }

        String token = authHeader.substring(7);
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("[ReportRestController] Token JWT invalide");
            return "";
        }

        try {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            return username != null ? username : "";
        } catch (Exception e) {
            log.warn("[ReportRestController] Impossible d'extraire le nom d'utilisateur du token JWT: {}", e.getMessage());
            return "";
        }
    }
}

