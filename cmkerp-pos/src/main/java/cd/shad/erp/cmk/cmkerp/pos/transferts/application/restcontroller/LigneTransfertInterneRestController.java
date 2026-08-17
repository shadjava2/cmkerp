package cd.shad.erp.cmk.cmkerp.pos.transferts.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_TRANSFERTS_INTERNES_BASE;

import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request.CreateLigneTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request.UpdateLigneTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.response.LigneTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.service.LigneTransfertInterneCommandService;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.service.LigneTransfertInterneQueryService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
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
 * Contrôleur REST pour la gestion des lignes de transfert interne (module POS).
 */
@RestController("posLigneTransfertInterneRestController")
@RequestMapping(POS_TRANSFERTS_INTERNES_BASE + "/{transfertInterneId}/lignes")
@RequiredArgsConstructor
@Tag(name = "POS - Lignes de Transfert Interne", description = "Gestion des lignes de transfert interne (module POS)")
@Validated
@Slf4j
public class LigneTransfertInterneRestController {

    private final LigneTransfertInterneQueryService ligneTransfertInterneQueryService;
    private final LigneTransfertInterneCommandService ligneTransfertInterneCommandService;
    private final JwtTokenProvider jwtTokenProvider;

    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }

    @GetMapping
    @Operation(summary = "Liste des lignes d'un transfert interne")
    public ResponseEntity<List<LigneTransfertInterneResponse>> findByFkTransfertInterne(
            @PathVariable Long transfertInterneId) {
        List<LigneTransfertInterneResponse> lignes =
                ligneTransfertInterneQueryService.findByFkTransfertInterne(transfertInterneId);
        return ResponseEntity.ok(lignes);
    }

    @GetMapping("/{ligneId}")
    @Operation(summary = "Récupère une ligne de transfert interne par son ID")
    public ResponseEntity<LigneTransfertInterneResponse> findById(@PathVariable Long ligneId) {
        LigneTransfertInterneResponse ligne = ligneTransfertInterneQueryService.findById(ligneId);
        return ResponseEntity.ok(ligne);
    }

    @PostMapping
    @Operation(summary = "Crée une nouvelle ligne de transfert interne")
    public ResponseEntity<LigneTransfertInterneResponse> create(
            @PathVariable Long transfertInterneId,
            @Valid @RequestBody CreateLigneTransfertInterneRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        LigneTransfertInterneResponse created =
                ligneTransfertInterneCommandService.create(transfertInterneId, request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{ligneId}")
    @Operation(summary = "Met à jour une ligne de transfert interne")
    public ResponseEntity<LigneTransfertInterneResponse> update(
            @PathVariable Long transfertInterneId,
            @PathVariable Long ligneId,
            @Valid @RequestBody UpdateLigneTransfertInterneRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        LigneTransfertInterneResponse updated =
                ligneTransfertInterneCommandService.update(transfertInterneId, ligneId, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{ligneId}")
    @Operation(summary = "Supprime une ligne de transfert interne")
    public ResponseEntity<Void> delete(
            @PathVariable Long transfertInterneId,
            @PathVariable Long ligneId) {
        ligneTransfertInterneCommandService.delete(transfertInterneId, ligneId);
        return ResponseEntity.noContent().build();
    }
}
