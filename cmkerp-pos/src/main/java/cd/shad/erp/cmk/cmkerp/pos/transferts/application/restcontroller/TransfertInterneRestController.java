package cd.shad.erp.cmk.cmkerp.pos.transferts.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.POS_TRANSFERTS_INTERNES_BASE;

import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.request.CreateTransfertInterneRequest;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.response.TransfertInterneResponse;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.service.TransfertInterneCommandService;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.service.TransfertInterneQueryService;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.service.TransfertInterneDestinationQueryService;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.PharmacieResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.dto.PageResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;

import cd.shad.erp.cmk.cmkerp.sharedkernel.security.AuthTokenExtractor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import cd.shad.erp.cmk.cmkerp.pos.transferts.application.service.TransfertInterneReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contrôleur REST pour la gestion des transferts internes (module POS).
 * Utilise les Query/Command Services de la nouvelle architecture DDD.
 */
@RestController("posTransfertInterneRestController")
@RequestMapping(POS_TRANSFERTS_INTERNES_BASE)
@RequiredArgsConstructor
@Tag(name = "POS - Transferts Internes", description = "Gestion des transferts internes de stock (module POS)")
@Validated
@Slf4j
public class TransfertInterneRestController {

    private final TransfertInterneQueryService transfertInterneQueryService;
    private final TransfertInterneCommandService transfertInterneCommandService;
    private final TransfertInterneDestinationQueryService destinationQueryService;
    private final TransfertInterneReportService transfertInterneReportService;
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
     * Génère un rapport PDF de la liste des transferts internes.
     */
    @GetMapping("/report")
    @Operation(summary = "Rapport PDF des transferts internes")
    public ResponseEntity<byte[]> generateListReport(
            @RequestParam(required = false) Long fkPharmacieSource,
            @RequestParam(required = false) Long fkPharmacieDestination,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String search) {
        try {
            List<TransfertInterneResponse> transferts = loadTransfertsForReport(
                    fkPharmacieSource, fkPharmacieDestination, statut, dateFrom, dateTo, search);
            String pharmacieNom = resolvePharmacieNom(fkPharmacieSource, fkPharmacieDestination, transferts);
            byte[] pdfBytes = transfertInterneReportService.generateListPdf(transferts, pharmacieNom);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "rapport-transferts-internes-pos-"
                    + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("inline", filename);
            headers.setContentLength(pdfBytes.length);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (JRException e) {
            log.error("Erreur JasperReports rapport transferts internes POS: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur JasperReports: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            log.error("Erreur rapport PDF transferts internes POS: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("Erreur: " + e.getMessage()).getBytes());
        }
    }

    /**
     * Génère un rapport Excel de la liste des transferts internes.
     */
    @GetMapping("/report/excel")
    @Operation(summary = "Rapport Excel des transferts internes")
    public ResponseEntity<byte[]> generateListReportExcel(
            @RequestParam(required = false) Long fkPharmacieSource,
            @RequestParam(required = false) Long fkPharmacieDestination,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String search) {
        List<TransfertInterneResponse> transferts = loadTransfertsForReport(
                fkPharmacieSource, fkPharmacieDestination, statut, dateFrom, dateTo, search);
        String pharmacieNom = resolvePharmacieNom(fkPharmacieSource, fkPharmacieDestination, transferts);
        byte[] excelBytes = transfertInterneReportService.generateListExcel(transferts, pharmacieNom);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        String filename = "rapport-transferts-internes-pos-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".xlsx";
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(excelBytes.length);
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
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

    /**
     * Récupère un transfert interne par son ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Récupère un transfert interne par son ID")
    public ResponseEntity<TransfertInterneResponse> findById(@PathVariable Long id) {
        TransfertInterneResponse transfert = transfertInterneQueryService.findById(id);
        return ResponseEntity.ok(transfert);
    }

    private List<TransfertInterneResponse> loadTransfertsForReport(
            Long fkPharmacieSource,
            Long fkPharmacieDestination,
            String statut,
            String dateFrom,
            String dateTo,
            String search) {
        Pageable pageable = PageRequest.of(0, 10000);
        PageResponse<TransfertInterneResponse> page = transfertInterneQueryService.findAll(
                pageable, fkPharmacieSource, fkPharmacieDestination, statut, search);
        LocalDateTime from = parseDateStart(dateFrom);
        LocalDateTime to = parseDateEnd(dateTo);
        return page.getContent().stream()
                .filter(t -> matchesDateRange(t, from, to))
                .collect(Collectors.toList());
    }

    private static boolean matchesDateRange(TransfertInterneResponse t, LocalDateTime from, LocalDateTime to) {
        if (t.getDateCreate() == null) {
            return from == null && to == null;
        }
        if (from != null && t.getDateCreate().isBefore(from)) {
            return false;
        }
        if (to != null && t.getDateCreate().isAfter(to)) {
            return false;
        }
        return true;
    }

    private static LocalDateTime parseDateStart(String dateFrom) {
        if (dateFrom == null || dateFrom.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateFrom).atStartOfDay();
    }

    private static LocalDateTime parseDateEnd(String dateTo) {
        if (dateTo == null || dateTo.isBlank()) {
            return null;
        }
        return LocalDate.parse(dateTo).atTime(23, 59, 59);
    }

    private static String resolvePharmacieNom(
            Long fkPharmacieSource, Long fkPharmacieDestination, List<TransfertInterneResponse> transferts) {
        if (fkPharmacieSource != null && !transferts.isEmpty()) {
            return transferts.stream()
                    .filter(t -> fkPharmacieSource.equals(t.getFkPharmacieSource()))
                    .map(TransfertInterneResponse::getPharmacieSourceNom)
                    .findFirst()
                    .orElse(null);
        }
        if (fkPharmacieDestination != null && !transferts.isEmpty()) {
            return transferts.stream()
                    .filter(t -> fkPharmacieDestination.equals(t.getFkPharmacieDestination()))
                    .map(TransfertInterneResponse::getPharmacieDestinationNom)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}





