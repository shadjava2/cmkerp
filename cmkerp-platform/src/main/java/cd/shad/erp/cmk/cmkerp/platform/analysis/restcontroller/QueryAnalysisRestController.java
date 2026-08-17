package cd.shad.erp.cmk.cmkerp.platform.analysis.restcontroller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cd.shad.erp.cmk.cmkerp.platform.analysis.service.SlowQueryAnalysisService;
import cd.shad.erp.cmk.cmkerp.platform.dto.response.SlowQueryAnalysisResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Contrôleur REST pour l'analyse des requêtes SQL et recommandations d'index.
 *
 * <p>
 * Expose les endpoints pour analyser le slow query log et générer des recommandations d'index
 * composites basées sur EXPLAIN.
 */
@RestController
@RequestMapping(ApiPaths.ADMIN_BASE + "/analysis")
@RequiredArgsConstructor
@Tag(name = "Admin - Analyse de Performance",
    description = "Analyse des requêtes SQL et recommandations d'index")
@Slf4j
public class QueryAnalysisRestController {

  private final SlowQueryAnalysisService slowQueryAnalysisService;

  /**
   * Analyse le top 20 des requêtes lentes et génère des recommandations d'index composites.
   *
   * <p>
   * Cette méthode :
   * <ul>
   * <li>Récupère le top 20 des requêtes les plus lentes depuis performance_schema</li>
   * <li>Exécute EXPLAIN pour chaque requête</li>
   * <li>Analyse les plans d'exécution</li>
   * <li>Génère des recommandations d'index composites</li>
   * <li>Fournit un résumé de l'analyse</li>
   * </ul>
   *
   * @return Analyse complète avec requêtes lentes et recommandations d'index
   */
  @GetMapping("/slow-queries")
  @Operation(
      summary = "Analyse le top 20 des requêtes lentes et génère des recommandations d'index",
      description = "Récupère les requêtes depuis performance_schema, exécute EXPLAIN et génère des recommandations d'index composites")
  public ResponseEntity<SlowQueryAnalysisResponse> analyzeTopSlowQueries() {
    log.info("Analyse du top 20 des requêtes lentes demandée");
    SlowQueryAnalysisResponse analysis = slowQueryAnalysisService.analyzeTopSlowQueries();
    return ResponseEntity.ok(analysis);
  }
}
