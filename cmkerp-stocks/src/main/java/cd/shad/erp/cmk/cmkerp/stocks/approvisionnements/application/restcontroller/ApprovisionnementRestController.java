package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.APPROVISIONNEMENTS_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.request.ApprovisionnementRequest;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.ApprovisionnementResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.LigneApprovResponse;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service.ApprovisionnementCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service.ApprovisionnementQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.service.LigneApprovQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.dto.request.DemandeAutorisationRequest;
import cd.shad.erp.cmk.cmkerp.stocks.autorisations.application.dto.response.AutorisationOperationResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.FournisseurQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FournisseurResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ReportService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des approvisionnements.
 * Utilise les Query/Command Services de la nouvelle architecture DDD.
 */
@RestController
@RequestMapping(APPROVISIONNEMENTS_BASE)
@RequiredArgsConstructor
@Tag(name = "Approvisionnements", description = "Gestion des approvisionnements (bons de livraison)")
@Validated
@Slf4j
public class ApprovisionnementRestController {

    private final ApprovisionnementQueryService approvisionnementQueryService;
    private final ApprovisionnementCommandService approvisionnementCommandService;
    private final LigneApprovQueryService ligneApprovQueryService;
    private final FournisseurQueryService fournisseurQueryService;
    private final ReportService reportService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère le userId depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }


    /**
     * Récupère une page d'approvisionnements avec pagination et filtres.
     */
    @GetMapping
    @Operation(summary = "Liste paginée des approvisionnements")
    public ResponseEntity<PageResponse<ApprovisionnementResponse>> findAll(
            Pageable pageable,
            @RequestParam(required = false) Long fkPharmacie,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long fkFournisseur,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String produit,
            @RequestParam(required = false) Long produitId) {
        PageResponse<ApprovisionnementResponse> approvisionnements = approvisionnementQueryService.findAll(
                pageable, fkPharmacie, statut, fkFournisseur, dateFrom, dateTo, search, produit, produitId);
        return ResponseEntity.ok(approvisionnements);
    }

    /**
     * Récupère un approvisionnement par son ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère un approvisionnement par son ID")
    public ResponseEntity<ApprovisionnementResponse> findById(@PathVariable Long id) {
        ApprovisionnementResponse approvisionnement = approvisionnementQueryService.findById(id);
        return ResponseEntity.ok(approvisionnement);
    }

    /**
     * Crée un nouvel approvisionnement.
     */
    @PostMapping
    @Operation(summary = "Crée un nouvel approvisionnement")
    public ResponseEntity<ApprovisionnementResponse> create(
            @Valid @RequestBody ApprovisionnementRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        ApprovisionnementResponse created = approvisionnementCommandService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour un approvisionnement existant.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Met à jour un approvisionnement")
    public ResponseEntity<ApprovisionnementResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ApprovisionnementRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        ApprovisionnementResponse updated = approvisionnementCommandService.update(id, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Valide un approvisionnement (passe le statut à VALIDEE).
     */
    @PostMapping("/{id}/valider")
    @Operation(summary = "Valide un approvisionnement")
    public ResponseEntity<Void> valider(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        approvisionnementCommandService.valider(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Annule un approvisionnement.
     * VALIDEE + stock OK → ANNULEE (retrait stock).
     * VALIDEE + stock consommé → ANNULEE SANS MODIFICATION (pas de retrait).
     * EN ATTENTE → ANNULEE SANS MODIFICATION.
     * Possible seulement dans les 24h après validation.
     */
    @PostMapping("/{id}/annuler")
    @Operation(summary = "Annule un approvisionnement (possible seulement dans les 24h après validation)")
    public ResponseEntity<Void> annuler(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        approvisionnementCommandService.annuler(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Demande une autorisation admin pour annuler un bon validé depuis plus de 24h.
     */
    @PostMapping("/{id}/demande-annulation")
    @Operation(summary = "Demande d'autorisation pour annulation tardive")
    public ResponseEntity<AutorisationOperationResponse> demanderAnnulation(
            @PathVariable Long id,
            @Valid @RequestBody DemandeAutorisationRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        AutorisationOperationResponse created =
                approvisionnementCommandService.demanderAnnulation(id, request.getMotif(), currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Génère un rapport PDF du bon d'approvisionnement avec toutes ses lignes.
     *
     * @param id ID de l'approvisionnement
     * @return PDF en streaming avec Content-Disposition: inline
     */
    @GetMapping("/{id}/report")
    @Operation(summary = "Génère un rapport PDF du bon d'approvisionnement")
    public ResponseEntity<byte[]> generateApprovisionnementReport(@PathVariable Long id) {
        log.info("🚀 [ApprovisionnementRestController] Début génération rapport approvisionnement - id: {}", id);

        try {
            // Récupérer l'approvisionnement et ses lignes
            ApprovisionnementResponse approvisionnement = approvisionnementQueryService.findById(id);
            List<cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.LigneApprovResponse> lignes =
                    ligneApprovQueryService.findByFkApprov(id);

            log.info("✅ [ApprovisionnementRestController] Approvisionnement {} récupéré avec {} lignes, génération du PDF...",
                    id, lignes.size());

            // Générer le rapport PDF
            byte[] pdfBytes = reportService.generateApprovisionnementReport(approvisionnement, lignes);

            log.info("✅ [ApprovisionnementRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline dans un iframe
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "bon-approvisionnement-" +
                    (approvisionnement.getNumbonliv() != null ? approvisionnement.getNumbonliv() : "N" + id) +
                    "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            log.info("✅ [ApprovisionnementRestController] Rapport approvisionnement généré avec succès: taille: {} bytes, filename: {}",
                    pdfBytes.length, filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport approvisionnement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération du rapport approvisionnement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur inattendue: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Génère un rapport Excel du bon d'approvisionnement avec toutes ses lignes.
     *
     * @param id ID de l'approvisionnement
     * @return Excel en streaming avec Content-Disposition: attachment
     */
    @GetMapping("/{id}/report/excel")
    @Operation(summary = "Génère un rapport Excel du bon d'approvisionnement")
    public ResponseEntity<byte[]> generateApprovisionnementReportExcel(@PathVariable Long id) {
        log.info("🚀 [ApprovisionnementRestController] Début génération rapport Excel approvisionnement - id: {}", id);

        try {
            // Récupérer l'approvisionnement et ses lignes
            ApprovisionnementResponse approvisionnement = approvisionnementQueryService.findById(id);
            List<cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.application.dto.response.LigneApprovResponse> lignes =
                    ligneApprovQueryService.findByFkApprov(id);

            log.info("✅ [ApprovisionnementRestController] Approvisionnement {} récupéré avec {} lignes, génération du Excel...",
                    id, lignes.size());

            // Générer le rapport Excel
            byte[] excelBytes = reportService.generateApprovisionnementReportExcel(approvisionnement, lignes);

            log.info("✅ [ApprovisionnementRestController] Excel généré: {} bytes", excelBytes.length);

            // Préparer les headers pour le téléchargement
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String filename = "bon-approvisionnement-" +
                    (approvisionnement.getNumbonliv() != null ? approvisionnement.getNumbonliv() : "N" + id) +
                    "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            log.info("✅ [ApprovisionnementRestController] Rapport Excel approvisionnement généré avec succès: taille: {} bytes, filename: {}",
                    excelBytes.length, filename);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération du rapport Excel approvisionnement: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur inattendue: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Génère un rapport PDF de la liste des approvisionnements avec leurs lignes.
     * Génère un seul rapport PDF contenant tous les approvisionnements de la page actuelle.
     *
     * @param fkPharmacie ID de la pharmacie (optionnel)
     * @param statut Filtre par statut (optionnel)
     * @param fkFournisseur Filtre par fournisseur (optionnel)
     * @param dateFrom Date de début (optionnel)
     * @param dateTo Date de fin (optionnel)
     * @param search Recherche par numéro de bon (optionnel)
     * @param page Numéro de page (requis)
     * @param size Taille de page (requis)
     * @return PDF en streaming avec Content-Disposition: inline
     */
    @GetMapping("/report")
    @Operation(summary = "Génère un rapport PDF de la liste des approvisionnements avec leurs lignes")
    public ResponseEntity<byte[]> generateApprovisionnementsListReport(
            @RequestParam(required = false) Long fkPharmacie,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long fkFournisseur,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String produit,
            @RequestParam(required = false) Long produitId,
            @RequestParam(required = true) Integer page,
            @RequestParam(required = true) Integer size) {

        log.info("🚀 [ApprovisionnementRestController] Début génération rapport liste approvisionnements - page={}, size={}",
                page, size);

        try {
            // Récupérer les approvisionnements de la page actuelle avec les mêmes filtres
            org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
            PageResponse<ApprovisionnementResponse> pageResponse = approvisionnementQueryService.findAll(
                    pageable, fkPharmacie, statut, fkFournisseur, dateFrom, dateTo, search, produit, produitId);
            List<ApprovisionnementResponse> approvisionnements = pageResponse.getContent();

            log.info("✅ [ApprovisionnementRestController] {} approvisionnements récupérés, récupération des lignes...",
                    approvisionnements.size());

            // Récupérer toutes les lignes pour tous les approvisionnements
            Map<Long, List<LigneApprovResponse>> approvIdToLignes = new HashMap<>();
            for (ApprovisionnementResponse approv : approvisionnements) {
                List<LigneApprovResponse> lignes = ligneApprovQueryService.findByFkApprov(approv.getId());
                approvIdToLignes.put(approv.getId(), lignes);
            }

            // Récupérer le nom de la pharmacie pour l'en-tête
            String pharmacieNom = null;
            if (fkPharmacie != null && !approvisionnements.isEmpty()) {
                pharmacieNom = approvisionnements.get(0).getPharmacieNom();
            }

            // Générer le rapport PDF
            byte[] pdfBytes = reportService.generateApprovisionnementsListReport(
                    approvisionnements, approvIdToLignes, pharmacieNom);

            log.info("✅ [ApprovisionnementRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline dans un iframe
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "rapport-approvisionnements-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            log.info("✅ [ApprovisionnementRestController] Rapport liste approvisionnements généré avec succès: taille: {} bytes, filename: {}",
                    pdfBytes.length, filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport liste approvisionnements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération du rapport liste approvisionnements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur inattendue: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Génère un rapport Excel de la liste des approvisionnements avec leurs lignes.
     * Génère un seul rapport Excel contenant tous les approvisionnements de la page actuelle.
     *
     * @param fkPharmacie ID de la pharmacie (optionnel)
     * @param statut Filtre par statut (optionnel)
     * @param fkFournisseur Filtre par fournisseur (optionnel)
     * @param dateFrom Date de début (optionnel)
     * @param dateTo Date de fin (optionnel)
     * @param search Recherche par numéro de bon (optionnel)
     * @param page Numéro de page (requis)
     * @param size Taille de page (requis)
     * @return Excel en streaming avec Content-Disposition: attachment
     */
    @GetMapping("/report/excel")
    @Operation(summary = "Génère un rapport Excel de la liste des approvisionnements avec leurs lignes")
    public ResponseEntity<byte[]> generateApprovisionnementsListReportExcel(
            @RequestParam(required = false) Long fkPharmacie,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long fkFournisseur,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String produit,
            @RequestParam(required = false) Long produitId,
            @RequestParam(required = true) Integer page,
            @RequestParam(required = true) Integer size) {

        log.info("🚀 [ApprovisionnementRestController] Début génération rapport Excel liste approvisionnements - page={}, size={}",
                page, size);

        try {
            // Récupérer les approvisionnements de la page actuelle avec les mêmes filtres
            org.springframework.data.domain.Pageable pageable = PageRequest.of(page, size);
            PageResponse<ApprovisionnementResponse> pageResponse = approvisionnementQueryService.findAll(
                    pageable, fkPharmacie, statut, fkFournisseur, dateFrom, dateTo, search, produit, produitId);
            List<ApprovisionnementResponse> approvisionnements = pageResponse.getContent();

            log.info("✅ [ApprovisionnementRestController] {} approvisionnements récupérés, récupération des lignes...",
                    approvisionnements.size());

            // Récupérer toutes les lignes pour tous les approvisionnements
            Map<Long, List<LigneApprovResponse>> approvIdToLignes = new HashMap<>();
            for (ApprovisionnementResponse approv : approvisionnements) {
                List<LigneApprovResponse> lignes = ligneApprovQueryService.findByFkApprov(approv.getId());
                approvIdToLignes.put(approv.getId(), lignes);
            }

            // Récupérer le nom de la pharmacie pour l'en-tête
            String pharmacieNom = null;
            if (fkPharmacie != null && !approvisionnements.isEmpty()) {
                pharmacieNom = approvisionnements.get(0).getPharmacieNom();
            }

            // Générer le rapport Excel
            byte[] excelBytes = reportService.generateApprovisionnementsListReportExcel(
                    approvisionnements, approvIdToLignes, pharmacieNom);

            log.info("✅ [ApprovisionnementRestController] Excel généré: {} bytes", excelBytes.length);

            // Préparer les headers pour le téléchargement
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String filename = "rapport-approvisionnements-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setContentLength(excelBytes.length);

            log.info("✅ [ApprovisionnementRestController] Rapport Excel liste approvisionnements généré avec succès: taille: {} bytes, filename: {}",
                    excelBytes.length, filename);

            return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);

        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération du rapport Excel liste approvisionnements: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport Excel: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur inattendue: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Récupère tous les fournisseurs (pour combo).
     * Endpoint de compatibilité avec l'ancien endpoint dans platform.approvisionnements.
     */
    @GetMapping("/fournisseurs")
    @Operation(summary = "Liste de tous les fournisseurs (pour combo)")
    public ResponseEntity<List<FournisseurResponse>> getAllFournisseurs() {
        List<FournisseurResponse> fournisseurs = fournisseurQueryService.findAllWithoutPagination();
        return ResponseEntity.ok(fournisseurs);
    }
}





