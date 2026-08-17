package cd.shad.erp.cmk.cmkerp.platform.inventory.application.restcontroller;

import static cd.shad.erp.cmk.cmkerp.sharedkernel.config.ApiPaths.INVENTORY_BASE;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.InventoryStatsResponse;
import cd.shad.erp.cmk.cmkerp.platform.inventory.application.service.InventoryDashboardQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Contrôleur REST pour le dashboard Inventory (Stock).
 *
 * <p>
 * Expose les endpoints de statistiques et métriques pour le module Stock.
 * Actuellement, retourne des valeurs par défaut jusqu'à l'implémentation complète.
 */
@RestController
@RequestMapping(INVENTORY_BASE + "/dashboard")
@RequiredArgsConstructor
@Tag(name = "Platform - Inventory Dashboard", description = "Statistiques et métriques du module Stock")
@Validated
public class InventoryDashboardRestController {

  private final InventoryDashboardQueryService inventoryDashboardQueryService;

  /**
   * Récupère les statistiques du dashboard Inventory.
   *
   * <p>
   * Retourne les métriques principales :
   * <ul>
   * <li>Rupture de stock</li>
   * <li>Produits périmés (dans 1 mois, 3 mois)</li>
   * <li>Achat risqué</li>
   * <li>Stock dormant</li>
   * <li>Stocks les plus/moins mouvementés</li>
   * <li>Fournisseurs</li>
   * <li>Demandes et réceptions en attente</li>
   * </ul>
   *
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies de l'utilisateur)
   * @return InventoryStatsResponse contenant toutes les statistiques
   */
  @GetMapping("/stats")
  @Operation(summary = "Récupère les statistiques du dashboard Inventory")
  public ResponseEntity<InventoryStatsResponse> getStats(
      @Parameter(description = "ID de la pharmacie (optionnel)")
      @RequestParam(required = false) Long pharmacieId) {

    InventoryStatsResponse stats = inventoryDashboardQueryService.getDashboardStats(pharmacieId);
    return ResponseEntity.ok(stats);
  }
}

