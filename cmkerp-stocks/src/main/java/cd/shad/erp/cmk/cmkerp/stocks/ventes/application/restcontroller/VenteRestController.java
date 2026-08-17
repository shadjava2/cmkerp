package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.VENTES_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.SortieUsageRequest;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.VenteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.VenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.LigneVenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service.VenteCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service.VenteQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service.LigneVenteQueryService;
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
 * Contrôleur REST pour la gestion des ventes (sorties pour usage).
 * Utilise les Query/Command Services de la nouvelle architecture DDD.
 */
@RestController
@RequestMapping(VENTES_BASE)
@RequiredArgsConstructor
@Tag(name = "Ventes", description = "Gestion des ventes (sorties pour usage)")
@Validated
@Slf4j
public class VenteRestController {

    private final VenteQueryService venteQueryService;
    private final VenteCommandService venteCommandService;
    private final LigneVenteQueryService ligneVenteQueryService;
    private final ReportService reportService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère le userId depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }


    /**
     * Récupère une page de ventes avec pagination et filtres.
     */
    @GetMapping
    @Operation(summary = "Liste paginée des ventes")
    public ResponseEntity<PageResponse<VenteResponse>> findAll(
            Pageable pageable,
            @RequestParam(required = false) Long fkPharmacie,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long fkPatient,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search) {
        PageResponse<VenteResponse> ventes = venteQueryService.findAll(
                pageable, fkPharmacie, statut, fkPatient, dateFrom, dateTo, search);
        return ResponseEntity.ok(ventes);
    }

    /**
     * Génère un rapport PDF de la liste des sorties pour usage avec leurs lignes.
     * Génère un seul rapport PDF contenant toutes les ventes de la page actuelle.
     * IMPORTANT: Cet endpoint doit être déclaré AVANT /{id} pour éviter les conflits de routage.
     *
     * @param fkPharmacie ID de la pharmacie (optionnel)
     * @param statut Filtre par statut (optionnel)
     * @param fkPatient Filtre par patient (optionnel)
     * @param dateFrom Date de début (optionnel)
     * @param dateTo Date de fin (optionnel)
     * @param search Recherche (optionnel)
     * @param page Numéro de page (requis)
     * @param size Taille de page (requis)
     * @return PDF en streaming avec Content-Disposition: inline
     */
    @GetMapping("/report")
    @Operation(summary = "Génère un rapport PDF de la liste des sorties pour usage avec leurs lignes")
    public ResponseEntity<byte[]> generateVentesListReport(
            @RequestParam(required = false) Long fkPharmacie,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) Long fkPatient,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String search,
            @RequestParam(required = true) Integer page,
            @RequestParam(required = true) Integer size) {

        log.info("🚀 [VenteRestController] Début génération rapport liste ventes - page={}, size={}, fkPharmacie={}, statut={}",
                page, size, fkPharmacie, statut);

        // Validation des paramètres requis
        if (page == null || size == null) {
            log.error("❌ [VenteRestController] Paramètres manquants: page={}, size={}", page, size);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(("Paramètres manquants: page et size sont requis").getBytes());
        }

        if (page < 0 || size <= 0) {
            log.error("❌ [VenteRestController] Paramètres invalides: page={}, size={}", page, size);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(("Paramètres invalides: page doit être >= 0 et size > 0").getBytes());
        }

        try {
            // Récupérer les ventes de la page actuelle avec les mêmes filtres
            PageRequest pageable = PageRequest.of(page, size);
            PageResponse<VenteResponse> pageResponse = venteQueryService.findAll(
                    pageable, fkPharmacie, statut, fkPatient, dateFrom, dateTo, search);
            List<VenteResponse> ventes = pageResponse.getContent();

            log.info("✅ [VenteRestController] {} ventes récupérées, récupération des lignes...",
                    ventes.size());

            // Récupérer toutes les lignes pour toutes les ventes
            Map<Long, List<LigneVenteResponse>> venteIdToLignes = new HashMap<>();
            for (VenteResponse vente : ventes) {
                List<LigneVenteResponse> lignes = ligneVenteQueryService.findByFkVente(vente.getId());
                venteIdToLignes.put(vente.getId(), lignes);
            }

            // Récupérer le nom de la pharmacie pour l'en-tête
            String pharmacieNom = null;
            if (fkPharmacie != null && !ventes.isEmpty()) {
                pharmacieNom = ventes.get(0).getPharmacieNom();
            }

            // Générer le rapport PDF
            byte[] pdfBytes = reportService.generateVentesListReport(ventes, venteIdToLignes, pharmacieNom);

            log.info("✅ [VenteRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline dans un iframe
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "liste-sorties-usage-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            log.info("✅ [VenteRestController] Rapport liste ventes généré avec succès: taille: {} bytes, filename: {}",
                    pdfBytes.length, filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport liste ventes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération du rapport liste ventes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur inattendue: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Récupère une vente par son ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère une vente par son ID")
    public ResponseEntity<VenteResponse> findById(@PathVariable Long id) {
        VenteResponse vente = venteQueryService.findById(id);
        return ResponseEntity.ok(vente);
    }

    /**
     * Crée une nouvelle vente.
     */
    @PostMapping
    @Operation(summary = "Crée une nouvelle vente")
    public ResponseEntity<VenteResponse> create(
            @Valid @RequestBody VenteRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        VenteResponse created = venteCommandService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour une vente existante.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Met à jour une vente")
    public ResponseEntity<VenteResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody VenteRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        VenteResponse updated = venteCommandService.update(id, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Valide une vente avec un statut dynamique.
     * Permet au frontend de spécifier le statut de validation selon le contexte :
     * <ul>
     *   <li><b>SORTIE-USAGE</b> : pour les sorties pour usage (module Stock)</li>
     *   <li><b>PAYEE</b> : pour les ventes payées (module POS)</li>
     *   <li><b>FACTUREE</b> : pour les ventes facturées</li>
     *   <li><b>VALIDEE</b> : alias de PAYEE pour compatibilité</li>
     * </ul>
     *
     * Si aucun statut n'est fourni, utilise <b>SORTIE-USAGE</b> par défaut (pour compatibilité).
     *
     * @param id ID de la vente à valider
     * @param statut Statut de validation à appliquer (optionnel, par défaut: SORTIE-USAGE)
     * @param httpRequest Requête HTTP pour extraire l'utilisateur courant
     * @return 204 No Content en cas de succès
     */
    @PostMapping("/{id}/valider")
    @Operation(
        summary = "Valide une vente avec un statut dynamique",
        description = "Valide une vente en spécifiant le statut de validation. " +
                     "Statuts possibles: SORTIE-USAGE (défaut), PAYEE, FACTUREE, VALIDEE. " +
                     "Permet une gestion flexible selon le contexte métier (sortie pour usage, vente POS, facturation, etc.)"
    )
    public ResponseEntity<Void> valider(
            @PathVariable Long id,
            @RequestParam(required = false) String statut,
            HttpServletRequest httpRequest) {
        log.info("🚀 [VenteRestController] Validation de la vente ID: {} avec statut: {}", id, statut != null ? statut : "SORTIE-USAGE (défaut)");
        Long currentUserId = getCurrentUserId(httpRequest);
        venteCommandService.valider(id, currentUserId, statut);
        log.info("✅ [VenteRestController] Vente {} validée avec succès", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Confirme une sortie pour usage (passe le statut à SORTIE-USAGE).
     * Même structure que {@link #annuler} : action dédiée, mise à jour stock, champs métier.
     */
    @PostMapping("/{id}/sortie-usage")
    @Operation(
        summary = "Confirme une sortie pour usage",
        description = "Passe le statut à SORTIE-USAGE, décrémente le stock (via SP_VALIDATE_VENTE), "
            + "et enregistre optionnellement raisonsortie / demandeur. Miroir de POST /{id}/annuler."
    )
    public ResponseEntity<Void> sortiePourUsage(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid SortieUsageRequest request,
            HttpServletRequest httpRequest) {
        log.info("🚀 [VenteRestController] Sortie pour usage de la vente ID: {}", id);
        Long currentUserId = getCurrentUserId(httpRequest);
        venteCommandService.sortiePourUsage(id, request, currentUserId);
        log.info("✅ [VenteRestController] Sortie pour usage confirmée pour vente {}", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Annule une vente (passe le statut à ANNULEE).
     * Possible seulement dans les 24h après validation.
     */
    @PostMapping("/{id}/annuler")
    @Operation(summary = "Annule une vente (possible seulement dans les 24h après validation)")
    public ResponseEntity<Void> annuler(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        venteCommandService.annuler(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Annule une vente avec remboursement (passe le statut à ANNULEE-REMBOURSE).
     * Possible seulement dans les 24h après validation.
     * Utilisé depuis la liste des ventes.
     */
    @PostMapping("/{id}/annuler-remboursement")
    @Operation(summary = "Annule une vente avec remboursement (possible seulement dans les 24h après validation)")
    public ResponseEntity<Void> annulerAvecRemboursement(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        venteCommandService.annulerAvecRemboursement(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Génère un rapport PDF de la sortie pour usage avec toutes ses lignes.
     *
     * @param id ID de la vente
     * @return PDF en streaming avec Content-Disposition: inline
     */
    @GetMapping("/{id}/report")
    @Operation(summary = "Génère un rapport PDF de la sortie pour usage")
    public ResponseEntity<byte[]> generateVenteReport(@PathVariable Long id) {
        log.info("🚀 [VenteRestController] Début génération rapport vente - id: {}", id);

        try {
            // Récupérer la vente et ses lignes
            VenteResponse vente = venteQueryService.findById(id);
            var lignes = ligneVenteQueryService.findByFkVente(id);

            log.info("✅ [VenteRestController] Vente {} récupérée avec {} lignes, génération du PDF...",
                    id, lignes.size());

            // Générer le rapport PDF
            byte[] pdfBytes = reportService.generateVenteReport(vente, lignes);

            log.info("✅ [VenteRestController] PDF généré: {} bytes", pdfBytes.length);

            // Préparer les headers pour l'affichage inline dans un iframe
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "sortie-usage-" + id + "-" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);

            log.info("✅ [VenteRestController] Rapport vente généré avec succès: taille: {} bytes, filename: {}",
                    pdfBytes.length, filename);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (JRException e) {
            log.error("Erreur JasperReports lors de la génération du rapport vente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (RuntimeException e) {
            log.error("Erreur lors de la génération du rapport vente: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur inattendue lors de la génération du rapport: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur inattendue: " + e.getMessage()).getBytes());
        }
    }

}





