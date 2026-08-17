package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_TRANSFERTS_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.TRANSFERTS_BASE;

import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ReportService;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateTransfertRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.ReplaceLigneTransfertRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.UpdateLigneTransfertRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.UpdateTransfertStatusRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneTransfertStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.TransfertStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service.TransfertStockCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service.TransfertStockQueryService;
import net.sf.jasperreports.engine.JRException;
import org.springframework.data.domain.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur REST pour la gestion des transferts de stock.
 */
@RestController
@RequestMapping({TRANSFERTS_BASE, POS_TRANSFERTS_BASE})
@RequiredArgsConstructor
@Tag(name = "Stocks - Transferts", description = "Gestion des transferts de stock")
@Validated
@Slf4j
public class TransfertStockRestController {

    private final TransfertStockQueryService transfertStockQueryService;
    private final TransfertStockCommandService transfertStockCommandService;
    private final ReportService reportService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère le userId depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }


    /**
     * Récupère une page de transferts avec pagination et filtres.
     */
    @GetMapping
    @Operation(summary = "Liste paginée des transferts")
    public ResponseEntity<PageResponse<TransfertStockResponse>> findAll(
            Pageable pageable,
            @RequestParam(required = false) Long fkPharmacieStock,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String search) {
        log.debug("Récupération des transferts - page: {}, size: {}, fkPharmacieStock: {}, statut: {}, search: {}",
                pageable.getPageNumber(), pageable.getPageSize(), fkPharmacieStock, statut, search);

        PageResponse<TransfertStockResponse> transferts = transfertStockQueryService.findAll(
                pageable, fkPharmacieStock, statut, search);
        return ResponseEntity.ok(transferts);
    }

    /**
     * Récupère un transfert par son ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère un transfert par son ID")
    public ResponseEntity<TransfertStockResponse> findById(@PathVariable Long id) {
        log.debug("Récupération du transfert - id: {}", id);
        TransfertStockResponse transfert = transfertStockQueryService.findById(id);
        return ResponseEntity.ok(transfert);
    }

    /**
     * Crée un nouveau transfert (traite une requête).
     */
    @PostMapping
    @Operation(summary = "Crée un nouveau transfert (traite une requête)")
    public ResponseEntity<TransfertStockResponse> create(
            @Valid @RequestBody CreateTransfertRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Création d'un transfert pour la requête - fkRequisition: {}", request.getFkRequisition());
        Long currentUserId = getCurrentUserId(httpRequest);
        TransfertStockResponse created = transfertStockCommandService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour le statut d'un transfert.
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "Met à jour le statut d'un transfert")
    public ResponseEntity<TransfertStockResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTransfertStatusRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Mise à jour du statut du transfert - id: {}, statut: {}", id, request.getStatut());
        Long currentUserId = getCurrentUserId(httpRequest);
        TransfertStockResponse updated = transfertStockCommandService.updateStatus(id, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Annule un transfert en attente de réception et restaure le stock source.
     */
    @PostMapping("/{id}/annuler")
    @Operation(summary = "Annule un transfert non réceptionné et restaure le stock")
    public ResponseEntity<TransfertStockResponse> annuler(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        log.debug("Annulation du transfert - id: {}", id);
        Long currentUserId = getCurrentUserId(httpRequest);
        TransfertStockResponse updated = transfertStockCommandService.annuler(id, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Récupère les lignes d'un transfert.
     */
    @GetMapping("/{id}/lignes")
    @Operation(summary = "Récupère les lignes d'un transfert")
    public ResponseEntity<List<LigneTransfertStockResponse>> getLignes(@PathVariable Long id) {
        log.debug("Récupération des lignes du transfert - id: {}", id);
        List<LigneTransfertStockResponse> lignes = transfertStockQueryService.findLignesByTransfertId(id);
        return ResponseEntity.ok(lignes);
    }

    /**
     * Met à jour une ligne de transfert.
     */
    @PutMapping("/{transfertId}/lignes/{ligneId}")
    @Operation(summary = "Met à jour une ligne de transfert")
    public ResponseEntity<LigneTransfertStockResponse> updateLigne(
            @PathVariable Long transfertId,
            @PathVariable Long ligneId,
            @Valid @RequestBody UpdateLigneTransfertRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Mise à jour de la ligne - transfertId: {}, ligneId: {}", transfertId, ligneId);
        Long currentUserId = getCurrentUserId(httpRequest);
        LigneTransfertStockResponse updated = transfertStockCommandService.updateLigne(
                transfertId, ligneId, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Remplace un produit dans une ligne de transfert.
     */
    @PutMapping("/{transfertId}/lignes/{ligneId}/replace")
    @Operation(summary = "Remplace un produit dans une ligne de transfert")
    public ResponseEntity<LigneTransfertStockResponse> replaceLigne(
            @PathVariable Long transfertId,
            @PathVariable Long ligneId,
            @Valid @RequestBody ReplaceLigneTransfertRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Remplacement du produit dans la ligne - transfertId: {}, ligneId: {}, nouveau fkStock: {}",
                transfertId, ligneId, request.getFkStock());
        Long currentUserId = getCurrentUserId(httpRequest);
        LigneTransfertStockResponse updated = transfertStockCommandService.replaceLigne(
                transfertId, ligneId, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime une ligne de transfert.
     */
    @DeleteMapping("/{transfertId}/lignes/{ligneId}")
    @Operation(summary = "Supprime une ligne de transfert")
    public ResponseEntity<Void> deleteLigne(
            @PathVariable Long transfertId,
            @PathVariable Long ligneId) {
        log.debug("Suppression de la ligne - transfertId: {}, ligneId: {}", transfertId, ligneId);
        transfertStockCommandService.deleteLigne(transfertId, ligneId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Génère le rapport PDF d'un transfert (bon de transfert).
     */
    @GetMapping("/{id}/report")
    @Operation(summary = "Génère un rapport PDF d'un transfert (bon de transfert)")
    public ResponseEntity<byte[]> generateTransfertReport(@PathVariable Long id) {
        log.info("🚀 [TransfertStockRestController] Début génération rapport transfert - id: {}", id);

        try {
            // Récupérer le transfert et ses lignes
            TransfertStockResponse transfert = transfertStockQueryService.findById(id);
            var lignes = transfertStockQueryService.findLignesByTransfertIdForReport(id);

            log.info("✅ [TransfertStockRestController] Transfert {} récupéré avec {} lignes", id, lignes.size());

            // Générer le rapport PDF
            byte[] pdfBytes = reportService.generateTransfertReport(transfert, lignes);

            log.info("✅ [TransfertStockRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "transfert-" + id + "-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport transfert: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport transfert: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Génère un rapport PDF général de la liste des transferts.
     */
    @GetMapping("/report")
    @Operation(summary = "Génère un rapport PDF général de la liste des transferts")
    public ResponseEntity<byte[]> generateTransfertsListReport(
            @RequestParam(required = false) Long fkPharmacieStock,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String search) {

        log.info("🚀 [TransfertStockRestController] Début génération rapport liste transferts");

        try {
            // Récupérer toutes les transferts (sans pagination pour le rapport)
            Pageable pageable = PageRequest.of(0, 10000);
            PageResponse<TransfertStockResponse> pageResponse = transfertStockQueryService.findAll(
                    pageable, fkPharmacieStock, statut, search);

            log.info("✅ [TransfertStockRestController] {} transferts récupérés, génération du PDF...",
                    pageResponse.getContent().size());

            // Récupérer toutes les lignes de chaque transfert
            Map<Long, List<LigneTransfertStockResponse>> transfertIdToLignes = new HashMap<>();
            for (TransfertStockResponse transf : pageResponse.getContent()) {
                try {
                    List<LigneTransfertStockResponse> lignes = transfertStockQueryService.findLignesByTransfertId(transf.getId());
                    if (lignes != null) {
                        transfertIdToLignes.put(transf.getId(), lignes);
                    } else {
                        log.warn("[TransfertStockRestController] ⚠️ Aucune ligne trouvée pour le transfert {}", transf.getId());
                        transfertIdToLignes.put(transf.getId(), new ArrayList<>());
                    }
                } catch (Exception e) {
                    log.error("[TransfertStockRestController] ❌ Erreur lors de la récupération des lignes pour le transfert {}: {}",
                            transf.getId(), e.getMessage(), e);
                    transfertIdToLignes.put(transf.getId(), new ArrayList<>());
                }
            }

            log.info("✅ [TransfertStockRestController] {} lignes récupérées au total pour {} transferts",
                    transfertIdToLignes.values().stream().mapToInt(List::size).sum(),
                    pageResponse.getContent().size());

            // Récupérer le nom de la pharmacie pour l'en-tête
            String pharmacieNom = null;
            if (fkPharmacieStock != null && !pageResponse.getContent().isEmpty()) {
                // Récupérer le nom depuis la première requisition associée
                pharmacieNom = pageResponse.getContent().get(0).getPharmacieDemandeurNom();
            }

            // Générer le rapport PDF avec les lignes détaillées
            byte[] pdfBytes = reportService.generateTransfertsListReport(
                    pageResponse.getContent(), transfertIdToLignes, pharmacieNom);

            log.info("✅ [TransfertStockRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "rapport-transferts-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport liste transferts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport liste transferts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Génère un rapport Excel de la liste des transferts.
     */
    @GetMapping("/report/excel")
    @Operation(summary = "Génère un rapport Excel de la liste des transferts")
    public ResponseEntity<byte[]> generateTransfertsReportExcel(
            @RequestParam(required = false) Long fkPharmacieStock,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String search) {

        log.info("🚀 [TransfertStockRestController] Début génération rapport Excel transferts");

        try {
            // Récupérer toutes les transferts (sans pagination pour le rapport Excel)
            Pageable pageable = PageRequest.of(0, 10000);
            PageResponse<TransfertStockResponse> pageResponse = transfertStockQueryService.findAll(
                    pageable, fkPharmacieStock, statut, search);

            log.info("✅ [TransfertStockRestController] {} transferts récupérés, génération du Excel...",
                    pageResponse.getContent().size());

            // Récupérer le nom de la pharmacie pour l'en-tête
            String pharmacieNom = null;
            if (fkPharmacieStock != null && !pageResponse.getContent().isEmpty()) {
                pharmacieNom = pageResponse.getContent().get(0).getPharmacieDemandeurNom();
            }

            // Générer le rapport Excel
            byte[] excelBytes = reportService.generateTransfertsReportExcel(
                    pageResponse.getContent(), pharmacieNom);

            log.info("✅ [TransfertStockRestController] Excel généré: {} bytes", excelBytes.length);

            // Préparer les headers pour le téléchargement
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String filename = "rapport-transferts-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport Excel transferts: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        }
    }
}





