package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.RECEPTIONS_TRANSFERTS_INTERNES_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.UpdateLigneReceptionTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.LigneReceptionTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service.LigneReceptionTransfertInterneCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service.LigneReceptionTransfertInterneQueryService;
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
 * Contrôleur REST pour la gestion des lignes de réception de transferts internes.
 */
@RestController
@RequestMapping(RECEPTIONS_TRANSFERTS_INTERNES_BASE + "/{receptionId}/lignes")
@RequiredArgsConstructor
@Tag(name = "Lignes Réceptions Transferts Internes", description = "Gestion des lignes de réception de transferts internes")
@Validated
@Slf4j
public class LigneReceptionTransfertInterneRestController {

    private final LigneReceptionTransfertInterneQueryService ligneReceptionTransfertInterneQueryService;
    private final LigneReceptionTransfertInterneCommandService ligneReceptionTransfertInterneCommandService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère le userId depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }


    /**
     * Récupère toutes les lignes d'une réception de transfert interne.
     */
    @GetMapping
    @Operation(summary = "Récupère toutes les lignes d'une réception de transfert interne")
    public ResponseEntity<List<LigneReceptionTransfertInterneResponse>> findByFkReceptionTransfertInterne(
            @PathVariable Long receptionId) {
        List<LigneReceptionTransfertInterneResponse> lignes = ligneReceptionTransfertInterneQueryService.findByFkReceptionTransfertInterne(receptionId);
        return ResponseEntity.ok(lignes);
    }

    /**
     * Récupère une ligne par son ID.
     */
    @GetMapping("/{ligneId}")
    @Operation(summary = "Récupère une ligne de réception par son ID")
    public ResponseEntity<LigneReceptionTransfertInterneResponse> findById(
            @PathVariable Long receptionId,
            @PathVariable Long ligneId) {
        LigneReceptionTransfertInterneResponse ligne = ligneReceptionTransfertInterneQueryService.findById(ligneId);
        return ResponseEntity.ok(ligne);
    }

    /**
     * Met à jour une ligne de réception de transfert interne.
     */
    @PutMapping("/{ligneId}")
    @Operation(summary = "Met à jour une ligne de réception de transfert interne")
    public ResponseEntity<LigneReceptionTransfertInterneResponse> update(
            @PathVariable Long receptionId,
            @PathVariable Long ligneId,
            @Valid @RequestBody UpdateLigneReceptionTransfertInterneRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Mise à jour de la ligne de réception - réceptionId: {}, ligneId: {}", receptionId, ligneId);
        Long currentUserId = getCurrentUserId(httpRequest);
        LigneReceptionTransfertInterneResponse ligne = ligneReceptionTransfertInterneCommandService.update(receptionId, ligneId, request, currentUserId);
        return ResponseEntity.ok(ligne);
    }

    /**
     * Supprime une ligne de réception de transfert interne.
     */
    @DeleteMapping("/{ligneId}")
    @Operation(summary = "Supprime une ligne de réception de transfert interne")
    public ResponseEntity<Void> delete(
            @PathVariable Long receptionId,
            @PathVariable Long ligneId) {
        log.debug("Suppression de la ligne de réception - réceptionId: {}, ligneId: {}", receptionId, ligneId);
        ligneReceptionTransfertInterneCommandService.delete(receptionId, ligneId);
        return ResponseEntity.noContent().build();
    }
}





