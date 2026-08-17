package cd.shad.erp.cmk.cmkerp.pos.dashboard.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_DASHBOARD_BASE;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.InventoryStatsResponse;
import cd.shad.erp.cmk.cmkerp.platform.inventory.application.dto.ProduitWithStockDTO;
import cd.shad.erp.cmk.cmkerp.platform.inventory.application.service.InventoryDashboardQueryService;
import cd.shad.erp.cmk.cmkerp.pos.dashboard.application.dto.response.StockMouvementResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.ProduitWithStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.exception.ErrorResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contrôleur REST pour le dashboard POS (statistiques stock).
 * Délègue aux services platform/stocks — même logique que StocksDashboardRestController.
 */
@RestController("posDashboardRestController")
@RequestMapping(POS_DASHBOARD_BASE)
@Tag(name = "POS - Dashboard", description = "Statistiques et métriques du module POS")
@Validated
@Slf4j
public class PosDashboardRestController {

  private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

  private final InventoryDashboardQueryService inventoryDashboardQueryService;
  private final ReportService reportService;
  private final JdbcTemplate jdbcTemplate;
  private final JwtTokenProvider jwtTokenProvider;

  public PosDashboardRestController(
      InventoryDashboardQueryService inventoryDashboardQueryService,
      ReportService reportService,
      @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
      JwtTokenProvider jwtTokenProvider) {
    this.inventoryDashboardQueryService = inventoryDashboardQueryService;
    this.reportService = reportService;
    this.jdbcTemplate = jdbcTemplate;
    this.jwtTokenProvider = jwtTokenProvider;
  }

  @GetMapping("/stats")
  @Operation(summary = "Statistiques du dashboard POS")
  public ResponseEntity<InventoryStatsResponse> getStats(
      @RequestParam(required = false) Long pharmacieId) {
    return ResponseEntity.ok(inventoryDashboardQueryService.getDashboardStats(pharmacieId));
  }

  @GetMapping("/stock-plus-mouvementes")
  @Operation(summary = "Stocks les plus mouvementés (POS)")
  public ResponseEntity<List<StockMouvementResponse>> getStockPlusMouvementes(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "10") Integer limit) {
    List<StockMouvementResponse> items = toStockMouvements(
        inventoryDashboardQueryService.getProductsForStat("stockPlusMouvementes", pharmacieId),
        "ENTREE", limit);
    return ResponseEntity.ok(items);
  }

  @GetMapping("/stock-moins-mouvementes")
  @Operation(summary = "Stocks les moins mouvementés (POS)")
  public ResponseEntity<List<StockMouvementResponse>> getStockMoinsMouvementes(
      @RequestParam(required = false) Long pharmacieId,
      @RequestParam(required = false, defaultValue = "10") Integer limit) {
    List<StockMouvementResponse> items = toStockMouvements(
        inventoryDashboardQueryService.getProductsForStat("stockMoinsMouvementes", pharmacieId),
        "SORTIE", limit);
    return ResponseEntity.ok(items);
  }

  @GetMapping("/stats/{statType}/report")
  @Operation(summary = "Rapport PDF d'une stat du dashboard POS")
  public ResponseEntity<byte[]> generateStatReport(
      @PathVariable String statType,
      @RequestParam(required = true) Long pharmacieId,
      HttpServletRequest request) {
    log.info("[PosDashboardRestController] Rapport PDF stat: {} (pharmacieId: {})", statType, pharmacieId);
    try {
      List<ProduitWithStockDTO> produitsDTO =
          inventoryDashboardQueryService.getProductsForStat(statType, pharmacieId);
      if (produitsDTO == null || produitsDTO.isEmpty()) {
        return buildErrorResponse("Aucun produit trouvé pour cette statistique", HttpStatus.NOT_FOUND);
      }
      List<ProduitWithStockResponse> produits = produitsDTO.stream()
          .map(this::toProduitWithStockResponse)
          .collect(Collectors.toList());
      String titreRapport = getStatTitle(statType);
      String pharmacieNom = getPharmacieNom(pharmacieId);
      String utilisateurNom = getCurrentUsername(request);
      byte[] pdfBytes = reportService.generateDashboardStatReport(
          produits, titreRapport, pharmacieNom, utilisateurNom);

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_PDF);
      headers.setContentDispositionFormData("inline",
          String.format("rapport-pos-%s-%s.pdf", statType,
              LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))));
      headers.setContentLength(pdfBytes.length);
      return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    } catch (JRException e) {
      log.error("[PosDashboardRestController] Erreur JasperReports: {}", e.getMessage(), e);
      return buildErrorResponse("Erreur JasperReports: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    } catch (Exception e) {
      log.error("[PosDashboardRestController] Erreur rapport: {}", e.getMessage(), e);
      return buildErrorResponse("Erreur: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @GetMapping("/stats/{statType}/report/excel")
  @Operation(summary = "Rapport Excel d'une stat du dashboard POS")
  public ResponseEntity<byte[]> generateStatReportExcel(
      @PathVariable String statType,
      @RequestParam(required = true) Long pharmacieId) {
    log.info("[PosDashboardRestController] Rapport Excel stat: {} (pharmacieId: {})", statType, pharmacieId);
    try {
      List<ProduitWithStockDTO> produitsDTO =
          inventoryDashboardQueryService.getProductsForStat(statType, pharmacieId);
      List<ProduitWithStockResponse> produits = produitsDTO.stream()
          .map(this::toProduitWithStockResponse)
          .collect(Collectors.toList());
      byte[] excelBytes = reportService.generateProduitsReportExcel(produits, getStatTitle(statType));

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.parseMediaType(
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
      headers.setContentDispositionFormData("attachment",
          String.format("rapport-pos-%s-%s.xlsx", statType,
              LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))));
      headers.setContentLength(excelBytes.length);
      return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    } catch (Exception e) {
      log.error("[PosDashboardRestController] Erreur Excel: {}", e.getMessage(), e);
      return buildErrorResponse("Erreur: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private List<StockMouvementResponse> toStockMouvements(
      List<ProduitWithStockDTO> produits, String mouvementType, Integer limit) {
    int max = limit != null && limit > 0 ? limit : 10;
    return produits.stream()
        .limit(max)
        .map(dto -> StockMouvementResponse.builder()
            .id(dto.getStockId() != null ? dto.getStockId() : dto.getId())
            .designation(dto.getNomcommercial())
            .quantite(dto.getStockencours())
            .mouvementType(mouvementType)
            .date(dto.getDateCreate() != null ? dto.getDateCreate().format(DATE_FMT) : null)
            .build())
        .collect(Collectors.toList());
  }

  private ProduitWithStockResponse toProduitWithStockResponse(ProduitWithStockDTO dto) {
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
  }

  private String getStatTitle(String statType) {
    return switch (statType) {
      case "ruptureStock" -> "Rupture de stock";
      case "perimeDans3Mois" -> "Périmé dans 3 mois";
      case "perimeDans1Mois" -> "Périmé dans 1 mois";
      case "achatConforme" -> "Achat Conforme (Faible Risque)";
      case "achatAcceptable" -> "Achat Acceptable (à surveiller)";
      case "achatRisqueEleve" -> "Achat avec risque élevé";
      case "achatNonConforme" -> "Achat non conforme (à refuser)";
      case "stockDormant" -> "Stock Dormant";
      case "stockPlusMouvementes" -> "Plus mouvementés";
      case "stockMoinsMouvementes" -> "Moins mouvementés";
      default -> "Rapport de stat";
    };
  }

  private String getPharmacieNom(Long pharmacieId) {
    if (pharmacieId == null) {
      return null;
    }
    try {
      return jdbcTemplate.queryForObject(
          "SELECT designation FROM pharmacies WHERE id = ?", String.class, pharmacieId);
    } catch (Exception e) {
      log.warn("[PosDashboardRestController] Pharmacie introuvable: {}", pharmacieId);
      return null;
    }
  }

  private String getCurrentUsername(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return "";
    }
    String token = authHeader.substring(7);
    if (!jwtTokenProvider.validateToken(token)) {
      return "";
    }
    try {
      String username = jwtTokenProvider.getUsernameFromToken(token);
      return username != null ? username : "";
    } catch (Exception e) {
      return "";
    }
  }

  private ResponseEntity<byte[]> buildErrorResponse(String message, HttpStatus status) {
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      ErrorResponse errorResponse = ErrorResponse.builder()
          .timestamp(LocalDateTime.now())
          .status(status.value())
          .error(status.getReasonPhrase())
          .message(message)
          .path("")
          .build();
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      return new ResponseEntity<>(objectMapper.writeValueAsBytes(errorResponse), headers, status);
    } catch (Exception e) {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      String fallbackJson = String.format("{\"message\":\"%s\",\"status\":%d}", message, status.value());
      return new ResponseEntity<>(fallbackJson.getBytes(), headers, status);
    }
  }
}
