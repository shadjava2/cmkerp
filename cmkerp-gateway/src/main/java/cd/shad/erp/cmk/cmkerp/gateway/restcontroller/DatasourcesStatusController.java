package cd.shad.erp.cmk.cmkerp.gateway.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.DATASOURCES_STATUS;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.platform.external.status.DatasourcesStatusService;
import cd.shad.erp.cmk.cmkerp.platform.external.status.DatasourcesStatusService.DatasourcesStatusReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Diagnostic des 3 bases. Toujours HTTP 200 : Mediline / CLINIQUE down n'est pas une panne API.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Gateway - Datasources", description = "Statut des connexions bases de données")
public class DatasourcesStatusController {

  private final DatasourcesStatusService datasourcesStatusService;

  @GetMapping(DATASOURCES_STATUS)
  @Operation(summary = "Statut des datasources",
      description = "Sonde cmkerp-v24prod, Mediline et CLINIQUE. Les bases externes ne bloquent pas l'API.")
  public ResponseEntity<DatasourcesStatusReport> status() {
    return ResponseEntity.ok(datasourcesStatusService.checkAll());
  }
}
