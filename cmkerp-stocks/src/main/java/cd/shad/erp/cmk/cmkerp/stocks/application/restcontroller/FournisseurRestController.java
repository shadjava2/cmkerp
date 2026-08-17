package cd.shad.erp.cmk.cmkerp.stocks.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_FOURNISSEURS_BASE;
import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.STOCKS_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.application.dto.request.FournisseurRequest;
import cd.shad.erp.cmk.cmkerp.stocks.application.dto.response.FournisseurResponse;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.FournisseurCommandService;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.FournisseurQueryService;
import cd.shad.erp.cmk.cmkerp.stocks.application.service.ReportService;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion des fournisseurs dans le module stocks.
 */
@RestController
@RequestMapping({STOCKS_BASE + "/fournisseurs", POS_FOURNISSEURS_BASE})
@RequiredArgsConstructor
@Tag(name = "Stocks - Fournisseurs", description = "Gestion des fournisseurs")
@Validated
@Slf4j
public class FournisseurRestController {

    private final FournisseurQueryService fournisseurQueryService;
    private final FournisseurCommandService fournisseurCommandService;
    private final ReportService reportService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Récupère une page de fournisseurs avec pagination.
     */
    @GetMapping
    @Operation(summary = "Liste paginée des fournisseurs")
    public ResponseEntity<PageResponse<FournisseurResponse>> findAll(
            Pageable pageable,
            @RequestParam(required = false) String nom) {
        // Créer un Pageable avec tri par défaut
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by("nom").ascending()
            );
        }
        PageResponse<FournisseurResponse> fournisseurs = fournisseurQueryService.findAll(pageable, nom);
        return ResponseEntity.ok(fournisseurs);
    }

    /**
     * Récupère un fournisseur par son ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère un fournisseur par son ID")
    public ResponseEntity<FournisseurResponse> findById(@PathVariable Long id) {
        FournisseurResponse fournisseur = fournisseurQueryService.findById(id);
        return ResponseEntity.ok(fournisseur);
    }

    /**
     * Crée un nouveau fournisseur.
     */
    @PostMapping
    @Operation(summary = "Crée un nouveau fournisseur")
    public ResponseEntity<FournisseurResponse> create(
            @Valid @RequestBody FournisseurRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        FournisseurResponse created = fournisseurCommandService.create(request, currentUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Met à jour un fournisseur existant.
     */
    @PutMapping("/{id}")
    @Operation(summary = "Met à jour un fournisseur")
    public ResponseEntity<FournisseurResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody FournisseurRequest request,
            HttpServletRequest httpRequest) {
        Long currentUserId = getCurrentUserId(httpRequest);
        FournisseurResponse updated = fournisseurCommandService.update(id, request, currentUserId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Supprime un fournisseur.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprime un fournisseur")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        fournisseurCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Génère un rapport PDF de la liste des fournisseurs.
     */
    @GetMapping("/report")
    @Operation(summary = "Génère un rapport PDF de la liste des fournisseurs")
    public ResponseEntity<byte[]> generateReport(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
            PageResponse<FournisseurResponse> fournisseurs = fournisseurQueryService.findAll(pageable, nom);

            byte[] pdfBytes = reportService.generateFournisseursReport(fournisseurs.getContent());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("inline", "rapport-fournisseurs.pdf");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport PDF des fournisseurs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Génère un rapport Excel de la liste des fournisseurs.
     */
    @GetMapping("/report/excel")
    @Operation(summary = "Génère un rapport Excel de la liste des fournisseurs")
    public ResponseEntity<byte[]> generateReportExcel(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by("nom").ascending());
            PageResponse<FournisseurResponse> fournisseurs = fournisseurQueryService.findAll(pageable, nom);

            byte[] excelBytes = reportService.generateFournisseursReportExcel(fournisseurs.getContent());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            headers.setContentDispositionFormData("attachment", "rapport-fournisseurs.xlsx");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(excelBytes);
        } catch (Exception e) {
            log.error("Erreur lors de la génération du rapport Excel des fournisseurs", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Extrait l'ID de l'utilisateur connecté depuis le JWT token.
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        return AuthTokenExtractor.getCurrentUserId(request, jwtTokenProvider);
    }

}





