package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_REQUISITIONS_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.REQUISITIONS_BASE;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ReportService;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.UpdateRequisitionStatusRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.RequisitionResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service.RequisitionCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service.RequisitionQueryService;
import net.sf.jasperreports.engine.JRException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneRequisitionReportDTO;

/**
 * Contrôleur REST pour la gestion des requisitions.
 */
@RestController
@RequestMapping({REQUISITIONS_BASE, POS_REQUISITIONS_BASE})
@RequiredArgsConstructor
@Tag(name = "Stocks - Requisitions", description = "Gestion des requisitions de stock")
@Validated
@Slf4j
public class RequisitionRestController {

    private final RequisitionQueryService requisitionQueryService;
    private final RequisitionCommandService requisitionCommandService;
    private final ReportService reportService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère le userId depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }


    /**
     * Récupère une page de requisitions avec pagination et filtres.
     */
    @GetMapping
    @Operation(summary = "Liste paginée des requisitions")
    public ResponseEntity<PageResponse<RequisitionResponse>> findAll(
            Pageable pageable,
            @RequestParam(required = false) Long fkPharmacieStock,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String search) {
        log.debug("Récupération des requisitions - page: {}, size: {}, fkPharmacieStock: {}, statut: {}, search: {}",
                pageable.getPageNumber(), pageable.getPageSize(), fkPharmacieStock, statut, search);

        PageResponse<RequisitionResponse> requisitions = requisitionQueryService.findAll(
                pageable, fkPharmacieStock, statut, search);
        return ResponseEntity.ok(requisitions);
    }

    /**
     * Récupère une requête par son ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère une requête par son ID")
    public ResponseEntity<RequisitionResponse> findById(@PathVariable Long id) {
        log.debug("Récupération de la requête - id: {}", id);
        RequisitionResponse requisition = requisitionQueryService.findById(id);
        return ResponseEntity.ok(requisition);
    }

    /**
     * Génère un rapport PDF d'une requisition (sans prix).
     */
    @GetMapping("/{id}/report")
    @Operation(summary = "Génère un rapport PDF d'une requisition (sans prix)")
    public ResponseEntity<byte[]> generateRequisitionReport(@PathVariable Long id) {
        log.info("🚀 [RequisitionRestController] Début génération rapport requisition (sans prix) - id: {}", id);

        try {
            // Récupérer la requisition et ses lignes
            RequisitionResponse requisition = requisitionQueryService.findById(id);
            var lignes = requisitionQueryService.findLignesByRequisitionId(id);

            log.info("✅ [RequisitionRestController] Requisition {} récupérée avec {} lignes", id, lignes.size());

            // Générer le rapport PDF
            byte[] pdfBytes = reportService.generateRequisitionReport(requisition, lignes);

            log.info("✅ [RequisitionRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "requisition-" + id + "-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport requisition: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport requisition: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Génère un rapport PDF d'une requisition (avec prix).
     */
    @GetMapping("/{id}/report/with-price")
    @Operation(summary = "Génère un rapport PDF d'une requisition (avec prix)")
    public ResponseEntity<byte[]> generateRequisitionReportWithPrice(@PathVariable Long id) {
        log.info("🚀 [RequisitionRestController] Début génération rapport requisition (avec prix) - id: {}", id);

        try {
            // Récupérer la requisition et ses lignes
            RequisitionResponse requisition = requisitionQueryService.findById(id);
            var lignes = requisitionQueryService.findLignesByRequisitionId(id);

            log.info("✅ [RequisitionRestController] Requisition {} récupérée avec {} lignes", id, lignes.size());

            // Générer le rapport PDF avec prix
            byte[] pdfBytes = reportService.generateRequisitionReportWithPrice(requisition, lignes);

            log.info("✅ [RequisitionRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "requisition-" + id + "-avec-prix-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport requisition avec prix: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport requisition avec prix: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Génère un rapport PDF général de la liste des requisitions.
     */
    @GetMapping("/report")
    @Operation(summary = "Génère un rapport PDF général de la liste des requisitions")
    public ResponseEntity<byte[]> generateRequisitionsListReport(
            @RequestParam(required = false) Long fkPharmacieStock,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String search,
            @RequestParam(required = true) Integer page,
            @RequestParam(required = true) Integer size) {

        log.info("🚀 [RequisitionRestController] Début génération rapport liste requisitions - page={}, size={}",
                page, size);

        try {
            // Récupérer les requisitions avec les mêmes filtres
            Pageable pageable = PageRequest.of(page, size);
            PageResponse<RequisitionResponse> pageResponse = requisitionQueryService.findAll(
                    pageable, fkPharmacieStock, statut, search);

            log.info("✅ [RequisitionRestController] {} requisitions récupérées (page {}), génération du PDF...",
                    pageResponse.getContent().size(), page);

            // Récupérer toutes les lignes de chaque requisition
            Map<Long, List<LigneRequisitionReportDTO>> requisitionIdToLignes = new HashMap<>();
            for (RequisitionResponse req : pageResponse.getContent()) {
                List<LigneRequisitionReportDTO> lignes = requisitionQueryService.findLignesByRequisitionId(req.getId());
                requisitionIdToLignes.put(req.getId(), lignes);
            }

            log.info("✅ [RequisitionRestController] {} lignes récupérées au total pour {} requisitions",
                    requisitionIdToLignes.values().stream().mapToInt(List::size).sum(),
                    pageResponse.getContent().size());

            // Récupérer le nom de la pharmacie pour l'en-tête
            String pharmacieNom = null;
            if (fkPharmacieStock != null && !pageResponse.getContent().isEmpty()) {
                pharmacieNom = pageResponse.getContent().get(0).getPharmacieStockNom();
            }

            // Générer le rapport PDF avec les lignes détaillées
            byte[] pdfBytes = reportService.generateRequisitionsListReport(
                    pageResponse.getContent(), requisitionIdToLignes, pharmacieNom);

            log.info("✅ [RequisitionRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "rapport-requisitions-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport liste requisitions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport liste requisitions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Génère un rapport Excel de la liste des requisitions.
     */
    @GetMapping("/report/excel")
    @Operation(summary = "Génère un rapport Excel de la liste des requisitions")
    public ResponseEntity<byte[]> generateRequisitionsReportExcel(
            @RequestParam(required = false) Long fkPharmacieStock,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String search) {

        log.info("🚀 [RequisitionRestController] Début génération rapport Excel requisitions");

        try {
            // Récupérer toutes les requisitions (sans pagination pour le rapport Excel)
            Pageable pageable = PageRequest.of(0, 10000);
            PageResponse<RequisitionResponse> pageResponse = requisitionQueryService.findAll(
                    pageable, fkPharmacieStock, statut, search);

            log.info("✅ [RequisitionRestController] {} requisitions récupérées, génération du Excel...",
                    pageResponse.getContent().size());

            // Récupérer le nom de la pharmacie pour l'en-tête
            String pharmacieNom = null;
            if (fkPharmacieStock != null && !pageResponse.getContent().isEmpty()) {
                pharmacieNom = pageResponse.getContent().get(0).getPharmacieStockNom();
            }

            // Générer le rapport Excel
            byte[] excelBytes = reportService.generateRequisitionsReportExcel(
                    pageResponse.getContent(), pharmacieNom);

            log.info("✅ [RequisitionRestController] Excel généré: {} bytes", excelBytes.length);

            // Préparer les headers pour le téléchargement
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String filename = "rapport-requisitions-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport Excel requisitions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Met à jour le statut d'une requisition (par exemple, pour la rejeter).
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Met à jour le statut d'une requisition")
    public ResponseEntity<RequisitionResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRequisitionStatusRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Mise à jour du statut de la requisition - id: {}, statut: {}", id, request.getStatut());
        Long currentUserId = getCurrentUserId(httpRequest);
        RequisitionResponse updated = requisitionCommandService.updateStatus(id, request, currentUserId);
        return ResponseEntity.ok(updated);
    }
}





