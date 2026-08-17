package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.INVENTAIRES_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request.InventaireLotsRequest;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request.LigneInventaireRequest;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.LigneInventaireResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.response.PerimableAlerteStockResponse;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.service.LigneInventaireCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.service.LigneInventaireQueryService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des lignes d'inventaire.
 * Utilise les Query/Command Services de la nouvelle architecture DDD.
 * Note: Les lignes sont créées automatiquement par une procédure stockée,
 * on ne peut que les mettre à jour.
 */
@RestController
@RequestMapping(INVENTAIRES_BASE + "/{inventaireId}/lignes")
@RequiredArgsConstructor
@Tag(name = "Lignes d'Inventaire", description = "Gestion des lignes d'inventaire")
@Validated
@Slf4j
public class LigneInventaireRestController {

    private final LigneInventaireQueryService ligneInventaireQueryService;
    private final LigneInventaireCommandService ligneInventaireCommandService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère le userId depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }


    /**
     * Liste des lignes d'inventaire.
     * <ul>
     *   <li>{@code operationnel=true} → uniquement stocks actifs ({@code WHERE operationnel = 1})</li>
     *   <li>{@code operationnel} absent → toutes les lignes</li>
     * </ul>
     */
    @GetMapping
    @Operation(summary = "Liste des lignes d'un inventaire (filtre optionnel operationnel)")
    public ResponseEntity<List<LigneInventaireResponse>> findByFkInventaire(
            @PathVariable Long inventaireId,
            @RequestParam(required = false) Boolean operationnel) {
        log.info("GET lignes inventaire id={} operationnel={}", inventaireId, operationnel);
        List<LigneInventaireResponse> lignes =
                ligneInventaireQueryService.findByFkInventaire(inventaireId, operationnel);
        return ResponseEntity.ok(lignes);
    }

    /**
     * Recherche une ligne par code-barres (scan mobile).
     */
    @GetMapping("/by-codebarre")
    @Operation(summary = "Trouve une ligne d'inventaire par code-barres")
    public ResponseEntity<LigneInventaireResponse> findByCodebarre(
            @PathVariable Long inventaireId,
            @RequestParam("code") String code) {
        LigneInventaireResponse ligne = ligneInventaireQueryService.findByCodebarreInInventaire(inventaireId, code);
        return ResponseEntity.ok(ligne);
    }

    /**
     * Liste les lots / dates / quantités saisis pour une ligne (perimable_alerte_stock).
     */
    @GetMapping("/{ligneId}/lots")
    @Operation(summary = "Lots et péremptions d'une ligne d'inventaire")
    public ResponseEntity<List<PerimableAlerteStockResponse>> getLots(
            @PathVariable Long inventaireId,
            @PathVariable Long ligneId) {
        return ResponseEntity.ok(ligneInventaireCommandService.getLotsForLigne(ligneId));
    }

    /**
     * Saisie multi-lots : enregistre lot + péremption + quantité par rayon, somme → quantité physique.
     */
    @PutMapping("/{ligneId}/lots")
    @Operation(summary = "Enregistre les lots inventoriés (somme = quantité physique)")
    public ResponseEntity<LigneInventaireResponse> updateLots(
            @PathVariable Long inventaireId,
            @PathVariable Long ligneId,
            @Valid @RequestBody InventaireLotsRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        LigneInventaireResponse updated =
                ligneInventaireCommandService.updateWithLots(ligneId, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Récupère une ligne d'inventaire par son ID.
     */
    @GetMapping("/{ligneId}")
    @Operation(summary = "Récupère une ligne d'inventaire par son ID")
    public ResponseEntity<LigneInventaireResponse> findById(@PathVariable Long ligneId) {
        LigneInventaireResponse ligne = ligneInventaireQueryService.findById(ligneId);
        return ResponseEntity.ok(ligne);
    }

    /**
     * Met à jour une ligne d'inventaire existante.
     * Note: Les lignes sont créées automatiquement par une procédure stockée,
     * on ne peut que les mettre à jour (quantité physique, commentaire).
     */
    @PutMapping("/{ligneId}")
    @Operation(summary = "Met à jour une ligne d'inventaire")
    public ResponseEntity<LigneInventaireResponse> update(
            @PathVariable Long inventaireId,
            @PathVariable Long ligneId,
            @Valid @RequestBody LigneInventaireRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        LigneInventaireResponse updated = ligneInventaireCommandService.update(ligneId, request, currentUserId);
        return ResponseEntity.ok(updated);
    }
}





