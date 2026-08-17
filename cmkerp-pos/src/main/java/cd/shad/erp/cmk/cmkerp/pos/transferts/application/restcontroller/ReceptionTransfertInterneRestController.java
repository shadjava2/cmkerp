package cd.shad.erp.cmk.cmkerp.pos.transferts.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_RECEPTIONS_TRANSFERTS_INTERNES_BASE;

import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request.CreateReceptionTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.response.ReceptionTransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.service.ReceptionTransfertInterneCommandService;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.service.ReceptionTransfertInterneQueryService;
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

/**
 * Contrôleur REST pour la gestion des réceptions de transferts internes (module POS).
 */
@RestController("posReceptionTransfertInterneRestController")
@RequestMapping(POS_RECEPTIONS_TRANSFERTS_INTERNES_BASE)
@RequiredArgsConstructor
@Tag(name = "POS - Réceptions Transferts Internes", description = "Gestion des réceptions de transferts internes (module POS)")
@Validated
@Slf4j
public class ReceptionTransfertInterneRestController {

    private final ReceptionTransfertInterneQueryService receptionTransfertInterneQueryService;
    private final ReceptionTransfertInterneCommandService receptionTransfertInterneCommandService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère le userId depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }


    /**
     * Récupère une page de réceptions de transferts internes avec pagination et filtres.
     */
    @GetMapping
    @Operation(summary = "Liste paginée des réceptions de transferts internes")
    public ResponseEntity<PageResponse<ReceptionTransfertInterneResponse>> findAll(
            Pageable pageable,
            @RequestParam(required = false) Long fkPharmacieDestination,
            @RequestParam(required = false) String statut) {
        PageResponse<ReceptionTransfertInterneResponse> receptions = receptionTransfertInterneQueryService.findAll(
                pageable, fkPharmacieDestination, statut);
        return ResponseEntity.ok(receptions);
    }

    /**
     * Récupère les transferts internes à réceptionner (statut TRANSFEREE et fkPharmacieDestination = pharmacie actuelle).
     */
    @GetMapping("/a-recevoir")
    @Operation(summary = "Liste paginée des transferts internes à réceptionner")
    public ResponseEntity<PageResponse<ReceptionTransfertInterneResponse>> findTransfertsInternesARecevoir(
            Pageable pageable,
            @RequestParam(required = true) Long fkPharmacieDestination) {
        PageResponse<ReceptionTransfertInterneResponse> transferts = receptionTransfertInterneQueryService.findTransfertsInternesARecevoir(
                pageable, fkPharmacieDestination);
        return ResponseEntity.ok(transferts);
    }

    /**
     * Récupère une réception de transfert interne par son ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère une réception de transfert interne par son ID")
    public ResponseEntity<ReceptionTransfertInterneResponse> findById(@PathVariable Long id) {
        ReceptionTransfertInterneResponse reception = receptionTransfertInterneQueryService.findById(id);
        return ResponseEntity.ok(reception);
    }

    /**
     * Récupère une réception de transfert interne par fkTransfertInterne.
     */
    @GetMapping("/by-transfert/{fkTransfertInterne}")
    @Operation(summary = "Récupère une réception de transfert interne par fkTransfertInterne")
    public ResponseEntity<ReceptionTransfertInterneResponse> findByFkTransfertInterne(@PathVariable Long fkTransfertInterne) {
        ReceptionTransfertInterneResponse reception = receptionTransfertInterneQueryService.findByFkTransfertInterne(fkTransfertInterne);
        return ResponseEntity.ok(reception);
    }

    /**
     * Crée une nouvelle réception de transfert interne.
     */
    @PostMapping
    @Operation(summary = "Crée une nouvelle réception de transfert interne")
    public ResponseEntity<ReceptionTransfertInterneResponse> create(
            @Valid @RequestBody CreateReceptionTransfertInterneRequest request,
            HttpServletRequest httpRequest) {
        log.debug("Création d'une réception de transfert interne pour transfert: {}", request.getFkTransfertInterne());
        Long currentUserId = getCurrentUserId(httpRequest);
        ReceptionTransfertInterneResponse created = receptionTransfertInterneCommandService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Réceptionne le transfert interne (passe le statut à RECEPTIONNEE).
     */
    @PostMapping("/{id}/receptionner")
    @Operation(summary = "Réceptionne le transfert interne")
    public ResponseEntity<ReceptionTransfertInterneResponse> receptionner(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        log.debug("Réception du transfert interne - réception ID: {}", id);
        Long currentUserId = getCurrentUserId(httpRequest);
        ReceptionTransfertInterneResponse reception = receptionTransfertInterneCommandService.receptionner(id, currentUserId);
        return ResponseEntity.ok(reception);
    }

    /**
     * Annule la réception (passe le statut à ANNULEE).
     */
    @PostMapping("/{id}/annuler")
    @Operation(summary = "Annule la réception")
    public ResponseEntity<ReceptionTransfertInterneResponse> annuler(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        log.debug("Annulation de la réception - réception ID: {}", id);
        Long currentUserId = getCurrentUserId(httpRequest);
        ReceptionTransfertInterneResponse reception = receptionTransfertInterneCommandService.annuler(id, currentUserId);
        return ResponseEntity.ok(reception);
    }
}





