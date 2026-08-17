package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.GMAO_DASHBOARD_BASE;

import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response.GmaoDashboardStatsResponse;
import cd.shad.erp.cmk.cmkerp.stocks.gmao.application.service.GmaoDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(GMAO_DASHBOARD_BASE)
@RequiredArgsConstructor
@Tag(name = "GMAO — Dashboard", description = "Indicateurs GMAO")
public class GmaoDashboardRestController {

  private final GmaoDashboardService gmaoDashboardService;

  @GetMapping("/stats")
  @Operation(summary = "Statistiques GMAO")
  public ResponseEntity<GmaoDashboardStatsResponse> stats() {
    return ResponseEntity.ok(gmaoDashboardService.stats());
  }
}
