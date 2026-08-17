package cd.shad.erp.cmk.cmkerp.platform.inventory.application.service;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.InventoryStatsResponse;
import cd.shad.erp.cmk.cmkerp.platform.inventory.application.dto.ProduitWithStockDTO;
import cd.shad.erp.cmk.cmkerp.platform.inventory.infrastructure.persistence.InventoryDashboardRepository;

/**
 * Query Service pour le dashboard Inventory (lecture uniquement).
 *
 * <p>Ce service contient toutes les opérations de lecture (queries) liées au dashboard Inventory.
 * Toutes les méthodes sont en lecture seule pour optimiser les performances.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InventoryDashboardQueryService {

  private final InventoryDashboardRepository inventoryDashboardRepository;

  /**
   * Récupère les statistiques du dashboard Inventory.
   *
   * <p>
   * Calcule toutes les métriques principales du dashboard :
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
   * @param pharmacieId filtre optionnel sur la pharmacie (null = toutes les pharmacies)
   * @return InventoryStatsResponse contenant toutes les statistiques
   */
  public InventoryStatsResponse getDashboardStats(Long pharmacieId) {
    log.debug("Récupération des statistiques du dashboard Inventory (pharmacieId: {})", pharmacieId);

    Integer ruptureStock = inventoryDashboardRepository.countRuptureStock(pharmacieId);
    Integer perimeDans3Mois = inventoryDashboardRepository.countPerimeDans3Mois(pharmacieId);
    Integer perimeDans1Mois = inventoryDashboardRepository.countPerimeDans1Mois(pharmacieId);
    Integer achatConforme = inventoryDashboardRepository.countAchatConforme(pharmacieId);
    Integer achatAcceptable = inventoryDashboardRepository.countAchatAcceptable(pharmacieId);
    Integer achatRisqueEleve = inventoryDashboardRepository.countAchatRisqueEleve(pharmacieId);
    Integer achatNonConforme = inventoryDashboardRepository.countAchatNonConforme(pharmacieId);
    Integer stockDormant = inventoryDashboardRepository.countStockDormant(pharmacieId);
    Integer stockPlusMouvementes = inventoryDashboardRepository.countStockPlusMouvementes(pharmacieId);
    Integer stockMoinsMouvementes = inventoryDashboardRepository.countStockMoinsMouvementes(pharmacieId);
    Integer fournisseurs = inventoryDashboardRepository.countFournisseurs();
    Integer demandesEnAttente = inventoryDashboardRepository.countDemandesEnAttente(pharmacieId);
    Integer receptionEnAttente = inventoryDashboardRepository.countReceptionEnAttente(pharmacieId);
    Integer produitsSuivis = inventoryDashboardRepository.countProduitsSuivis(pharmacieId);

    log.debug("Statistiques calculées - Rupture: {}, Périmés (3m): {}, Périmés (1m): {}, "
            + "Achat Conforme: {}, Achat Acceptable: {}, Achat Risque Élevé: {}, Achat Non Conforme: {}, "
            + "Dormant: {}, Plus mouvementés: {}, Moins mouvementés: {}, "
            + "Fournisseurs: {}, Demandes: {}, Réceptions: {}, Produits suivis: {}",
        ruptureStock, perimeDans3Mois, perimeDans1Mois, achatConforme, achatAcceptable, achatRisqueEleve, achatNonConforme,
        stockDormant, stockPlusMouvementes, stockMoinsMouvementes, fournisseurs, demandesEnAttente, receptionEnAttente,
        produitsSuivis);

    return new InventoryStatsResponse(
        ruptureStock,
        perimeDans3Mois,
        perimeDans1Mois,
        achatConforme,
        achatAcceptable,
        achatRisqueEleve,
        achatNonConforme,
        stockDormant,
        stockPlusMouvementes,
        stockMoinsMouvementes,
        fournisseurs,
        demandesEnAttente,
        receptionEnAttente,
        produitsSuivis
    );
  }

  /**
   * Récupère la liste des produits pour une stat spécifique du dashboard.
   *
   * @param statType type de stat (ruptureStock, perimeDans3Mois, etc.)
   * @param pharmacieId filtre optionnel sur la pharmacie
   * @return liste des produits correspondant à la stat
   */
  public List<ProduitWithStockDTO> getProductsForStat(String statType, Long pharmacieId) {
    log.debug("Récupération des produits pour stat: {} (pharmacieId: {})", statType, pharmacieId);

    return switch (statType) {
      case "ruptureStock" -> inventoryDashboardRepository.findRuptureStock(pharmacieId);
      case "perimeDans3Mois", "expireBientot" -> inventoryDashboardRepository.findPerimeDans3Mois(pharmacieId);
      case "perimeDans1Mois" -> inventoryDashboardRepository.findPerimeDans1Mois(pharmacieId);
      case "achatConforme" -> inventoryDashboardRepository.findAchatConforme(pharmacieId);
      case "achatAcceptable" -> inventoryDashboardRepository.findAchatAcceptable(pharmacieId);
      case "achatRisqueEleve" -> inventoryDashboardRepository.findAchatRisqueEleve(pharmacieId);
      case "achatNonConforme" -> inventoryDashboardRepository.findAchatNonConforme(pharmacieId);
      case "stockDormant" -> inventoryDashboardRepository.findStockDormant(pharmacieId);
      case "stockPlusMouvementes" -> inventoryDashboardRepository.findStockPlusMouvementes(pharmacieId);
      case "stockMoinsMouvementes" -> inventoryDashboardRepository.findStockMoinsMouvementes(pharmacieId);
      case "produitsSuivis" -> inventoryDashboardRepository.findProduitsSuivis(pharmacieId);
      default -> {
        log.warn("Type de stat inconnu: {}", statType);
        yield List.of();
      }
    };
  }
}

