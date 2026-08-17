package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.VENTES_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request.LigneVenteRequest;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response.LigneVenteResponse;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service.LigneVenteCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.ventes.application.service.LigneVenteQueryService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des lignes de vente.
 * Utilise les Query/Command Services de la nouvelle architecture DDD.
 */
@RestController
@RequestMapping(VENTES_BASE + "/{venteId}/lignes")
@RequiredArgsConstructor
@Tag(name = "Lignes de Vente", description = "Gestion des lignes de vente")
@Validated
@Slf4j
public class LigneVenteRestController {

    private final LigneVenteQueryService ligneVenteQueryService;
    private final LigneVenteCommandService ligneVenteCommandService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère le userId depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }


    /**
     * Récupère toutes les lignes d'une vente.
     */
    @GetMapping
    @Operation(summary = "Liste des lignes d'une vente")
    public ResponseEntity<List<LigneVenteResponse>> findByFkVente(@PathVariable Long venteId) {
        List<LigneVenteResponse> lignes = ligneVenteQueryService.findByFkVente(venteId);
        return ResponseEntity.ok(lignes);
    }

    /**
     * Récupère une ligne de vente par son ID.
     */
    @GetMapping("/{ligneId}")
    @Operation(summary = "Récupère une ligne de vente par son ID")
    public ResponseEntity<LigneVenteResponse> findById(@PathVariable Long ligneId) {
        LigneVenteResponse ligne = ligneVenteQueryService.findById(ligneId);
        return ResponseEntity.ok(ligne);
    }

    /**
     * Crée une nouvelle ligne de vente.
     */
    @PostMapping
    @Operation(summary = "Crée une nouvelle ligne de vente")
    public ResponseEntity<LigneVenteResponse> create(
            @PathVariable Long venteId,
            @Valid @RequestBody LigneVenteRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        // S'assurer que le fkVente dans la requête correspond au path variable
        request.setFkVente(venteId);
        LigneVenteResponse created = ligneVenteCommandService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour une ligne de vente existante.
     */
    @PutMapping("/{ligneId}")
    @Operation(summary = "Met à jour une ligne de vente")
    public ResponseEntity<LigneVenteResponse> update(
            @PathVariable Long venteId,
            @PathVariable Long ligneId,
            @Valid @RequestBody LigneVenteRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        // S'assurer que le fkVente dans la requête correspond au path variable
        request.setFkVente(venteId);
        LigneVenteResponse updated = ligneVenteCommandService.update(ligneId, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime une ligne de vente.
     */
    @DeleteMapping("/{ligneId}")
    @Operation(summary = "Supprime une ligne de vente")
    public ResponseEntity<Void> delete(@PathVariable Long ligneId) {
        ligneVenteCommandService.delete(ligneId);
        return ResponseEntity.noContent().build();
    }
}





