package cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_BASE;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.InventoryStatsResponse;
import cd.shad.erp.cmk.cmkerp.platform.inventory.application.dto.ProduitWithStockDTO;
import cd.shad.erp.cmk.cmkerp.platform.inventory.application.service.InventoryDashboardQueryService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitWithStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.exception.ErrorResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.stream.Collectors;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;

/**
 * Contrôleur REST pour le dashboard Stocks.
 *
 * <p>
 * Expose les endpoints de statistiques et métriques pour le module Stock.
 * Délègue au service InventoryDashboardQueryService pour la logique métier.
 */
@RestController
@RequestMapping(STOCKS_BASE + "/dashboard")
@RequiredArgsConstructor
@Tag(name = "Stocks - Dashboard", description = "Statistiques et métriques du module Stock")
@Validated
@Slf4j
public class StocksDashboardRestController {

  private final InventoryDashboardQueryService inventoryDashboardQueryService;
  private final ReportService reportService;
  @Qualifier("primaryJdbcTemplate")
  private final JdbcTemplate jdbcTemplate;
  private final JwtTokenProvider jwtTokenProvider;

  /**
   * Récupère les statistiques du dashboard Stocks.
   *
   * <p>
   * Retourne les métriques principales :
   * <ul>
   * <li>Rupture de stock</li>
   * <li>Produits périmés (dans 1 mois, 3 mois)</li>
   * <li>Achat conforme, acceptable, risque élevé, non conforme</li>
   * <li>Stock dormant</li>
   * <li>Stocks les plus/moins mouvementés</li>
   * <li>Fournisseurs</li>
   * <li>Demandes et réceptions en attente</li>
   * </ul>
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies de l'utilisateur)
   * @return InventoryStatsResponse contenant toutes les statistiques
   */
  @GetMapping("/stats")
  @Operation(summary = "Récupère les statistiques du dashboard Stocks")
  public ResponseEntity<InventoryStatsResponse> getStats(
      @Parameter(description = "ID de la pharmacie (optionnel)")
      @RequestParam(required = false) Long pharmacieId) {

    InventoryStatsResponse stats = inventoryDashboardQueryService.getDashboardStats(pharmacieId);
    return ResponseEntity.ok(stats);
  }

  @GetMapping("/stats/{statType}/produits")
  @Operation(summary = "Liste détaillée des produits pour une KPI du dashboard")
  public ResponseEntity<List<ProduitWithStockResponse>> getProduitsForStat(
      @Parameter(description = "Type de stat (ruptureStock, perimeDans1Mois, etc.)")
      @PathVariable String statType,
      @Parameter(description = "ID de la pharmacie (optionnel)")
      @RequestParam(required = false) Long pharmacieId) {

    List<ProduitWithStockDTO> produitsDTO =
        inventoryDashboardQueryService.getProductsForStat(statType, pharmacieId);
    List<ProduitWithStockResponse> produits = produitsDTO.stream()
        .map(this::toProduitWithStockResponse)
        .collect(Collectors.toList());
    return ResponseEntity.ok(produits);
  }

  /**
   * Génère un rapport PDF pour une stat spécifique du dashboard.
   *
   * @param statType type de stat (ruptureStock, perimeDans3Mois, etc.)
   * @param pharmacieId ID de la pharmacie (requis)
   * @return PDF en streaming avec Content-Disposition: inline
   */
  @GetMapping("/stats/{statType}/report")
  @Operation(summary = "Génère un rapport PDF pour une stat du dashboard")
  public ResponseEntity<byte[]> generateStatReport(
      @Parameter(description = "Type de stat (ruptureStock, perimeDans3Mois, etc.)")
      @PathVariable String statType,
      @Parameter(description = "ID de la pharmacie (requis)")
      @RequestParam(required = true) Long pharmacieId,
      HttpServletRequest request) {

    log.info("🚀 [StocksDashboardRestController] Génération rapport pour stat: {} (pharmacieId: {})", statType, pharmacieId);

    try {
      // Récupérer les produits pour cette stat (DTO du platform)
      log.debug("[StocksDashboardRestController] Récupération des produits pour stat: {}", statType);
      List<ProduitWithStockDTO> produitsDTO = inventoryDashboardQueryService.getProductsForStat(statType, pharmacieId);
      log.debug("[StocksDashboardRestController] {} produits récupérés", produitsDTO != null ? produitsDTO.size() : 0);

      if (produitsDTO == null || produitsDTO.isEmpty()) {
        log.warn("[StocksDashboardRestController] Aucun produit trouvé pour stat: {}", statType);
        return buildErrorResponse("Aucun produit trouvé pour cette statistique", HttpStatus.NOT_FOUND);
      }

      // Convertir les DTOs en ProduitWithStockResponse pour le service de rapport
      log.debug("[StocksDashboardRestController] Conversion des DTOs en ProduitWithStockResponse...");
      List<ProduitWithStockResponse> produits = produitsDTO.stream()
          .map(this::toProduitWithStockResponse)
          .collect(Collectors.toList());
      log.debug("[StocksDashboardRestController] {} produits convertis", produits.size());

      // Déterminer le titre du rapport
      String titreRapport = getStatTitle(statType);
      log.debug("[StocksDashboardRestController] Génération du rapport PDF: {}", titreRapport);

      // Récupérer le nom de la pharmacie
      String pharmacieNom = getPharmacieNom(pharmacieId);
      log.debug("[StocksDashboardRestController] Pharmacie: {}", pharmacieNom);

      // Récupérer le nom de l'utilisateur connecté
      String utilisateurNom = getCurrentUsername(request);
      log.debug("[StocksDashboardRestController] Utilisateur: {}", utilisateurNom);

      // Générer le rapport PDF avec le template dédié aux stats du dashboard
      byte[] pdfBytes = reportService.generateDashboardStatReport(produits, titreRapport, pharmacieNom, utilisateurNom);

      // Préparer les headers avec optimisations de cache et compression
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      String filename = String.format("rapport-%s-%s.pdf", statType,
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
      headers.setContentDispositionFormData("inline", filename);
      headers.setContentLength(pdfBytes.length);
      // Optimisations de performance
      headers.setCacheControl("no-cache, no-store, must-revalidate"); // Pas de cache pour les rapports dynamiques
      headers.setPragma("no-cache");
      headers.setExpires(0);
      // Compression Gzip activée automatiquement par Spring Boot si configurée
      // La compression est gérée par le serveur (Tomcat/Undertow) selon la configuration

      log.info("✅ [StocksDashboardRestController] Rapport généré: {} produits, taille: {} bytes", produits.size(), pdfBytes.length);

      return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

    } catch (JRException e) {
      log.error("Erreur JasperReports lors de la génération du rapport: {}", e.getMessage(), e);
      String errorMsg = extractReadableErrorMessage(e);
      return buildErrorResponse("Erreur JasperReports: " + errorMsg, HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (org.springframework.dao.DataAccessException e) {
      log.error("Erreur d'accès aux données lors de la génération du rapport: {}", e.getMessage(), e);
      String errorMsg = extractReadableErrorMessage(e);
      return buildErrorResponse("Erreur de base de données: " + errorMsg, HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      log.error("Erreur lors de la génération du rapport: {}", e.getMessage(), e);
      String errorMsg = extractReadableErrorMessage(e);
      return buildErrorResponse("Erreur: " + errorMsg, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Génère un rapport Excel pour une stat spécifique du dashboard.
   *
   * @param statType type de stat (ruptureStock, perimeDans3Mois, etc.)
   * @param pharmacieId ID de la pharmacie (requis)
   * @return Excel en streaming avec Content-Disposition: attachment
   */
  @GetMapping("/stats/{statType}/report/excel")
  @Operation(summary = "Génère un rapport Excel pour une stat du dashboard")
  public ResponseEntity<byte[]> generateStatReportExcel(
      @Parameter(description = "Type de stat (ruptureStock, perimeDans3Mois, etc.)")
      @PathVariable String statType,
      @Parameter(description = "ID de la pharmacie (requis)")
      @RequestParam(required = true) Long pharmacieId) {

    log.info("🚀 [StocksDashboardRestController] Génération rapport Excel pour stat: {} (pharmacieId: {})", statType, pharmacieId);

    try {
      // Récupérer les produits pour cette stat (DTO du platform)
      List<ProduitWithStockDTO> produitsDTO = inventoryDashboardQueryService.getProductsForStat(statType, pharmacieId);

      // Convertir les DTOs en ProduitWithStockResponse pour le service de rapport
      List<ProduitWithStockResponse> produits = produitsDTO.stream()
          .map(this::toProduitWithStockResponse)
          .collect(Collectors.toList());

      // Déterminer le titre du rapport
      String titreRapport = getStatTitle(statType);
      log.debug("[StocksDashboardRestController] Génération du rapport Excel: {}", titreRapport);

      // Générer le rapport Excel
      byte[] excelBytes = reportService.generateProduitsReportExcel(produits, titreRapport);

      // Préparer les headers (même logique que les autres rapports produits)
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
      String filename = String.format("rapport-%s-%s.xlsx", statType,
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")));
      headers.setContentDispositionFormData("attachment", filename);
      headers.setContentLength(excelBytes.length);

      log.info("✅ [StocksDashboardRestController] Rapport Excel généré: {} produits, taille: {} bytes", produits.size(), excelBytes.length);

      return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

    } catch (Exception e) {
      log.error("Erreur lors de la génération du rapport Excel: {}", e.getMessage(), e);
      String errorMsg = extractReadableErrorMessage(e);
      return buildErrorResponse("Erreur: " + errorMsg, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Retourne le titre du rapport selon le type de stat.
   */
  private String getStatTitle(String statType) {
    return switch (statType) {
      case "ruptureStock" -> "Rupture de stock";
      case "perimeDans3Mois", "expireBientot" -> "Expire bientôt (< 3 mois)";
      case "perimeDans1Mois" -> "Périmé dans 1 mois";
      case "achatConforme" -> "Achat Conforme (Faible Risque)";
      case "achatAcceptable" -> "Achat Acceptable (à surveiller)";
      case "achatRisqueEleve" -> "Achat avec risque élevé";
      case "achatNonConforme" -> "Achat non conforme (à refuser)";
      case "stockDormant" -> "Stock Dormant";
      case "stockPlusMouvementes" -> "Plus mouvementés";
      case "stockMoinsMouvementes" -> "Moins mouvementés";
      case "produitsSuivis" -> "Produits suivis";
      default -> "Rapport de stat";
    };
  }

  /**
   * Convertit un ProduitWithStockDTO (platform) en ProduitWithStockResponse (stocks).
   */
  private ProduitWithStockResponse toProduitWithStockResponse(ProduitWithStockDTO dto) {
    if (dto == null) {
      log.warn("[StocksDashboardRestController] DTO null lors de la conversion");
      throw new IllegalArgumentException("DTO ne peut pas être null");
    }

    try {
      return ProduitWithStockResponse.builder()
          .id(dto.getId())
          .codebarre(dto.getCodebarre())
          .nomcommercial(dto.getNomcommercial())
          .nomscientifique(dto.getNomscientifique())
          .forme(dto.getForme())
          .dosage(dto.getDosage())
          .conditionnement(dto.getConditionnement())
          .categorie(dto.getCategorie())
          .stockId(dto.getStockId())
          .stockencours(dto.getStockencours())
          .isactif(dto.getIsactif() != null ? dto.getIsactif() : false)
          .peremption(dto.getPeremption())
          .prixachat(dto.getPrixachat())
          .qtealert(dto.getQtealert())
          .qtcritique(dto.getQtcritique())
          .perimable(dto.getPerimable() != null ? dto.getPerimable() : false)
          .dateCreate(dto.getDateCreate())
          .dateApprov(dto.getDateApprov())
          .build();
    } catch (Exception e) {
      log.error("[StocksDashboardRestController] Erreur lors de la conversion DTO -> Response pour produit ID: {}", dto.getId(), e);
      throw new RuntimeException("Erreur lors de la conversion du produit: " + e.getMessage(), e);
    }
  }

  /**
   * Récupère le nom de la pharmacie par son ID.
   */
  private String getPharmacieNom(Long pharmacieId) {
    if (pharmacieId == null) {
      return null;
    }
    String sql = "SELECT designation FROM pharmacies WHERE id = ?";
    try {
      return jdbcTemplate.queryForObject(sql, String.class, pharmacieId);
    } catch (Exception e) {
      log.warn("[StocksDashboardRestController] Pharmacie non trouvée pour ID: {}", pharmacieId);
      return null;
    }
  }

  /**
   * Récupère le nom d'utilisateur depuis le JWT token.
   */
  private String getCurrentUsername(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.warn("[StocksDashboardRestController] Utilisateur non authentifié");
      return "";
    }

    String token = authHeader.substring(7);
    if (!jwtTokenProvider.validateToken(token)) {
      log.warn("[StocksDashboardRestController] Token JWT invalide");
      return "";
    }

    try {
      String username = jwtTokenProvider.getUsernameFromToken(token);
      return username != null ? username : "";
    } catch (Exception e) {
      log.warn("[StocksDashboardRestController] Impossible d'extraire le nom d'utilisateur du token JWT: {}", e.getMessage());
      return "";
    }
  }

  /**
   * Extrait un message d'erreur lisible depuis une exception.
   * Évite d'inclure des requêtes SQL complètes ou des stack traces.
   */
  private String extractReadableErrorMessage(Exception e) {
    String message = e.getMessage();
    if (message == null) {
      return "Erreur inconnue";
    }

    // Si le message contient une requête SQL, essayer d'extraire seulement la partie erreur
    if (message.contains("PreparedStatementCallback") || message.contains("SQLException")) {
      // Extraire la partie après "SQLException" ou "for SQL"
      int sqlIndex = message.indexOf("for SQL");
      if (sqlIndex > 0) {
        // Prendre la partie avant "for SQL" qui contient généralement le message d'erreur
        String beforeSql = message.substring(0, sqlIndex).trim();
        // Chercher le dernier ";" ou ":" pour isoler le message d'erreur
        int lastColon = beforeSql.lastIndexOf(":");
        if (lastColon > 0 && lastColon < beforeSql.length() - 1) {
          return beforeSql.substring(lastColon + 1).trim();
        }
        return beforeSql;
      }

      // Si on trouve "SQLException", extraire le message après
      int sqlExceptionIndex = message.indexOf("SQLException");
      if (sqlExceptionIndex > 0) {
        String afterException = message.substring(sqlExceptionIndex + "SQLException".length()).trim();
        // Prendre jusqu'à 200 caractères ou jusqu'à "for SQL"
        int forSqlIndex = afterException.indexOf("for SQL");
        if (forSqlIndex > 0) {
          return afterException.substring(0, Math.min(forSqlIndex, 200)).trim();
        }
        return afterException.length() > 200 ? afterException.substring(0, 200) + "..." : afterException;
      }
    }

    // Limiter la longueur du message pour éviter les messages trop longs
    if (message.length() > 500) {
      return message.substring(0, 500) + "...";
    }

    return message;
  }

  /**
   * Construit une réponse d'erreur en JSON.
   */
  private ResponseEntity<byte[]> buildErrorResponse(String message, HttpStatus status) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      // Créer un objet ErrorResponse valide
      ErrorResponse errorResponse = ErrorResponse.builder()
          .timestamp(LocalDateTime.now())
          .status(status.value())
          .error(status.getReasonPhrase())
          .message(message)
          .path("") // Le path n'est pas disponible ici
          .build();

      // Sérialiser en JSON avec support pour LocalDateTime
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule()); // Support pour LocalDateTime
      byte[] jsonBytes = objectMapper.writeValueAsBytes(errorResponse);

      return new ResponseEntity<>(jsonBytes, headers, status);
    } catch (Exception e) {
      log.error("Erreur lors de la construction de la réponse d'erreur", e);
      // Fallback: retourner un JSON simple
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      String fallbackJson = String.format("{\"error\":\"%s\",\"message\":\"%s\",\"status\":%d}",
          status.getReasonPhrase(), message, status.value());
      return new ResponseEntity<>(fallbackJson.getBytes(), headers, status);
    }
  }
}



