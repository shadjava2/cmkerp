package cd.shad.erp.cmk.cmkerp.platform.approvisionnements.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.APPROVISIONNEMENTS_BASE;

import cd.shad.erp.cmk.cmkerp.platform.approvisionnements.application.dto.response.EchangeDeviseResponse;
import cd.shad.erp.cmk.cmkerp.platform.approvisionnements.application.service.EchangeDeviseQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des échanges de devise (pour combo).
 */
@RestController
@RequestMapping(APPROVISIONNEMENTS_BASE + "/echange-devises")
@RequiredArgsConstructor
@Tag(name = "Approvisionnements - EchangeDevises", description = "Gestion des échanges de devise (pour combo)")
@Validated
@Slf4j
public class EchangeDeviseRestController {

    private final EchangeDeviseQueryService echangeDeviseQueryService;

    /**
     * Récupère tous les échanges de devise (pour combo).
     */
    @GetMapping
    @Operation(summary = "Liste de tous les échanges de devise (pour combo)")
    public ResponseEntity<List<EchangeDeviseResponse>> findAll() {
        List<EchangeDeviseResponse> echangeDevises = echangeDeviseQueryService.findAll();
        return ResponseEntity.ok(echangeDevises);
    }
}



