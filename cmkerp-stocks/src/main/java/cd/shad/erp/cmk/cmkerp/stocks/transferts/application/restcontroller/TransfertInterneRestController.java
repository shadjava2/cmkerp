package cd.shad.erp.cmk.cmkerp.stocks.transferts.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_TRANSFERTS_INTERNES_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.TRANSFERTS_INTERNES_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.request.CreateTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.dto.response.TransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service.TransfertInterneCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service.TransfertInterneQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.transferts.application.service.TransfertInterneDestinationQueryService;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des transferts internes.
 * Utilise les Query/Command Services de la nouvelle architecture DDD.
 */
@RestController
@RequestMapping({TRANSFERTS_INTERNES_BASE, POS_TRANSFERTS_INTERNES_BASE})
@RequiredArgsConstructor
@Tag(name = "Transferts Internes", description = "Gestion des transferts internes de stock (alias POS)")
@Validated
@Slf4j
public class TransfertInterneRestController {

    private final TransfertInterneQueryService transfertInterneQueryService;
    private final TransfertInterneCommandService transfertInterneCommandService;
    private final TransfertInterneDestinationQueryService destinationQueryService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère le userId depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }


    /**
     * Récupère une page de transferts internes avec pagination et filtres.
     */
    @GetMapping
    @Operation(summary = "Liste paginée des transferts internes")
    public ResponseEntity<PageResponse<TransfertInterneResponse>> findAll(
            Pageable pageable,
            @RequestParam(required = false) Long fkPharmacieSource,
            @RequestParam(required = false) Long fkPharmacieDestination,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String search) {
        PageResponse<TransfertInterneResponse> transferts = transfertInterneQueryService.findAll(
                pageable, fkPharmacieSource, fkPharmacieDestination, statut, search);
        return ResponseEntity.ok(transferts);
    }

    /**
     * Récupère un transfert interne par son ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère un transfert interne par son ID")
    public ResponseEntity<TransfertInterneResponse> findById(@PathVariable Long id) {
        TransfertInterneResponse transfert = transfertInterneQueryService.findById(id);
        return ResponseEntity.ok(transfert);
    }

    /**
     * Récupère les pharmacies destinations éligibles pour un transfert interne.
     */
    @GetMapping("/destinations-eligibles")
    @Operation(summary = "Récupère les pharmacies destinations éligibles")
    public ResponseEntity<List<PharmacieResponse>> findDestinationsEligibles(
            @RequestParam(required = true) Long sourcePharmacieId) {
        List<PharmacieResponse> pharmacies = destinationQueryService.findDestinationsEligibles(sourcePharmacieId);
        return ResponseEntity.ok(pharmacies);
    }

    /**
     * Crée un nouveau transfert interne.
     */
    @PostMapping
    @Operation(summary = "Crée un nouveau transfert interne")
    public ResponseEntity<TransfertInterneResponse> create(
            @Valid @RequestBody CreateTransfertInterneRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        TransfertInterneResponse created = transfertInterneCommandService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Valide un transfert interne (passe le statut à TRANSFEREE).
     */
    @PostMapping("/{id}/valider")
    @Operation(summary = "Valide un transfert interne")
    public ResponseEntity<Void> valider(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        transfertInterneCommandService.valider(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Annule un transfert interne (passe le statut à ANNULEE).
     */
    @PostMapping("/{id}/annuler")
    @Operation(summary = "Annule un transfert interne")
    public ResponseEntity<Void> annuler(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        transfertInterneCommandService.annuler(id, currentUserId);
        return ResponseEntity.noContent().build();
    }
}





