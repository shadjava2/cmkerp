package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.INVENTAIRES_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request.InventaireRequest;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.InventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.LigneInventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.service.InventaireCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.service.InventaireQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.service.LigneInventaireQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ReportService;
import net.sf.jasperreports.engine.JRException;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Contrôleur REST pour la gestion des inventaires.
 * Utilise les Query/Command Services de la nouvelle architecture DDD.
 */
@RestController
@RequestMapping(INVENTAIRES_BASE)
@RequiredArgsConstructor
@Tag(name = "Inventaires", description = "Gestion des inventaires")
@Validated
@Slf4j
public class InventaireRestController {

    private final InventaireQueryService inventaireQueryService;
    private final InventaireCommandService inventaireCommandService;
    private final LigneInventaireQueryService ligneInventaireQueryService;
    private final ReportService reportService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère le userId depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }


    /**
     * Récupère une page d'inventaires avec pagination et filtres.
     */
    @GetMapping
    @Operation(summary = "Liste paginée des inventaires")
    public ResponseEntity<PageResponse<InventaireResponse>> findAll(
            Pageable pageable,
            @RequestParam(required = false) Long fkPharmacie,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String typeinventaire,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search) {
        PageResponse<InventaireResponse> inventaires = inventaireQueryService.findAll(
                pageable, fkPharmacie, statut, typeinventaire, dateFrom, dateTo, search);
        return ResponseEntity.ok(inventaires);
    }

    /**
     * Récupère un inventaire par son ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère un inventaire par son ID")
    public ResponseEntity<InventaireResponse> findById(@PathVariable Long id) {
        InventaireResponse inventaire = inventaireQueryService.findById(id);
        return ResponseEntity.ok(inventaire);
    }

    /**
     * Crée un nouvel inventaire.
     */
    @PostMapping
    @Operation(summary = "Crée un nouvel inventaire")
    public ResponseEntity<InventaireResponse> create(
            @Valid @RequestBody InventaireRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        InventaireResponse created = inventaireCommandService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour un inventaire existant.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Met à jour un inventaire")
    public ResponseEntity<InventaireResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody InventaireRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        InventaireResponse updated = inventaireCommandService.update(id, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Termine un inventaire (passe le statut à TERMINE et met à jour date_fin).
     */
    @PostMapping("/{id}/terminer")
    @Operation(summary = "Termine un inventaire")
    public ResponseEntity<Void> terminer(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        inventaireCommandService.terminer(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Annule un inventaire (passe le statut à ANNULE).
     */
    @PostMapping("/{id}/annuler")
    @Operation(summary = "Annule un inventaire")
    public ResponseEntity<Void> annuler(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        inventaireCommandService.annuler(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Génère un rapport PDF de la liste des inventaires avec leurs lignes.
     * Génère un seul rapport PDF contenant tous les inventaires de la page actuelle.
     * IMPORTANT: Cet endpoint doit être déclaré AVANT /{id} pour éviter les conflits de routage.
     *
     * @param fkPharmacie ID de la pharmacie (optionnel)
     * @param statut Filtre par statut (optionnel)
     * @param typeinventaire Filtre par type d'inventaire (optionnel)
     * @param dateFrom Date de début (optionnel)
     * @param dateTo Date de fin (optionnel)
     * @param search Recherche (optionnel)
     * @param page Numéro de page (requis)
     * @param size Taille de page (requis)
     * @return PDF en streaming avec Content-Disposition: inline
     */
    @GetMapping("/report")
    @Operation(summary = "Génère un rapport PDF de la liste des inventaires avec leurs lignes")
    public ResponseEntity<byte[]> generateInventairesListReport(
            @RequestParam(required = false) Long fkPharmacie,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String typeinventaire,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = true) Integer page,
            @RequestParam(required = true) Integer size) {

        log.info("🚀 [InventaireRestController] Début génération rapport liste inventaires - page={}, size={}, fkPharmacie={}, statut={}",
                page, size, fkPharmacie, statut);

        // Validation des paramètres requis
        if (page == null || size == null) {
            log.error("❌ [InventaireRestController] Paramètres manquants: page={}, size={}", page, size);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(("Paramètres manquants: page et size sont requis").getBytes());
        }

        if (page < 0 || size <= 0) {
            log.error("❌ [InventaireRestController] Paramètres invalides: page={}, size={}", page, size);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(("Paramètres invalides: page doit être >= 0 et size > 0").getBytes());
        }

        try {
            // Récupérer les inventaires de la page actuelle avec les mêmes filtres
            PageRequest pageable = PageRequest.of(page, size);
            PageResponse<InventaireResponse> pageResponse = inventaireQueryService.findAll(
                    pageable, fkPharmacie, statut, typeinventaire, dateFrom, dateTo, search);
            List<InventaireResponse> inventaires = pageResponse.getContent();

            log.info("✅ [InventaireRestController] {} inventaires récupérés, récupération des lignes...",
                    inventaires.size());

            // Récupérer toutes les lignes pour tous les inventaires
            Map<Long, List<LigneInventaireResponse>> inventaireIdToLignes = new HashMap<>();
            for (InventaireResponse inventaire : inventaires) {
                List<LigneInventaireResponse> lignes = ligneInventaireQueryService.findByFkInventaire(inventaire.getId());
                inventaireIdToLignes.put(inventaire.getId(), lignes);
            }

            // Récupérer le nom de la pharmacie pour l'en-tête
            String pharmacieNom = null;
            if (fkPharmacie != null && !inventaires.isEmpty()) {
                pharmacieNom = inventaires.get(0).getPharmacieNom();
            }

            // Générer le rapport PDF
            byte[] pdfBytes = reportService.generateInventairesListReport(inventaires, inventaireIdToLignes, pharmacieNom);

            log.info("✅ [InventaireRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline dans un iframe
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "liste-inventaires-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            log.info("✅ [InventaireRestController] Rapport liste inventaires généré avec succès: taille: {} bytes, filename: {}",
                    pdfBytes.length, filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport liste inventaires: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération du rapport liste inventaires: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur inattendue: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Génère un rapport PDF d'un inventaire avec toutes ses lignes.
     *
     * @param id ID de l'inventaire
     * @return PDF en streaming avec Content-Disposition: inline
     */
    @GetMapping("/{id}/report")
    @Operation(summary = "Génère un rapport PDF d'un inventaire")
    public ResponseEntity<byte[]> generateInventaireReport(@PathVariable Long id) {
        log.info("🚀 [InventaireRestController] Début génération rapport inventaire - id: {}", id);

        try {
            // Récupérer l'inventaire et ses lignes
            InventaireResponse inventaire = inventaireQueryService.findById(id);
            var lignes = ligneInventaireQueryService.findByFkInventaire(id);

            log.info("✅ [InventaireRestController] Inventaire {} récupéré avec {} lignes, génération du PDF...",
                    id, lignes.size());

            // Générer le rapport PDF
            byte[] pdfBytes = reportService.generateInventaireReport(inventaire, lignes);

            log.info("✅ [InventaireRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline dans un iframe
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "inventaire-" + id + "-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            log.info("✅ [InventaireRestController] Rapport inventaire généré avec succès: taille: {} bytes, filename: {}",
                    pdfBytes.length, filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport inventaire: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération du rapport inventaire: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur inattendue: " + e.getMessage()).getBytes());
        }
    }
}





