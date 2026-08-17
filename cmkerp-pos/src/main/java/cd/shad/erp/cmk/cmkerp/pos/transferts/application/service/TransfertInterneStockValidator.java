package cd.shad.erp.cmk.cmkerp.pos.transferts.application.service;

import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Validations stock partagées pour les transferts internes POS.
 */
@Component("posTransfertInterneStockValidator")
@Slf4j
public class TransfertInterneStockValidator {

  private final JdbcTemplate jdbcTemplate;

  public TransfertInterneStockValidator(@Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void verifyStockExists(Long fkStock) {
    String sql = "SELECT COUNT(*) FROM stock_produits WHERE id = ?";
    Long count = jdbcTemplate.queryForObject(sql, Long.class, fkStock);
    if (count == null || count == 0) {
      throw NotFoundException.entity("Stock", fkStock);
    }
  }

  public void verifyStockNotExpired(Long fkStock) {
    String sql = """
        SELECT COUNT(*) FROM perimable_alerte_stock
        WHERE fkStock = ? AND notifactif = TRUE AND dateperemtion <= CURDATE()
        """;
    Long count = jdbcTemplate.queryForObject(sql, Long.class, fkStock);
    if (count != null && count > 0) {
      throw new BusinessException("Impossible d'utiliser un stock périmé pour un transfert interne");
    }
  }

  public void verifyStockAvailable(Long fkStock, Float quantity, Long fkPharmacieSource) {
    verifyStockAvailable(fkStock, quantity, fkPharmacieSource, null, null, 0f);
  }

  public void verifyStockAvailable(Long fkStock, Float quantity, Long fkPharmacieSource,
      Long fkTransfertInterne, Long excludeLigneId) {
    verifyStockAvailable(fkStock, quantity, fkPharmacieSource, fkTransfertInterne, excludeLigneId, 0f);
  }

  /**
   * @param extraReservedQty quantités déjà réservées dans le même lot (création multi-lignes)
   */
  public void verifyStockAvailable(Long fkStock, Float quantity, Long fkPharmacieSource,
      Long fkTransfertInterne, Long excludeLigneId, float extraReservedQty) {
    if (fkStock == null || quantity == null) {
      return;
    }

    String stockSql = "SELECT qte FROM stock_produits WHERE id = ? AND fkPharmacies = ?";
    Float stockDisponible;
    try {
      stockDisponible = jdbcTemplate.queryForObject(stockSql, Float.class, fkStock, fkPharmacieSource);
    } catch (Exception e) {
      log.error("Erreur lors de la récupération du stock pour ID: {} - {}", fkStock, e.getMessage());
      throw new BusinessException("Impossible de récupérer le stock disponible");
    }

    if (stockDisponible == null) {
      throw new BusinessException("Stock introuvable pour le produit dans la pharmacie source");
    }
    if (stockDisponible < 0) {
      throw new BusinessException(
          String.format("Le stock ne peut pas être négatif. Stock actuel: %.2f", stockDisponible));
    }

    float quantiteTotaleAutresLignes = extraReservedQty;
    if (fkTransfertInterne != null) {
      String lignesSql = excludeLigneId != null
          ? "SELECT COALESCE(SUM(quantite), 0) FROM lignes_transfert_interne WHERE fkStock = ? AND fkTransfertInterne = ? AND id != ?"
          : "SELECT COALESCE(SUM(quantite), 0) FROM lignes_transfert_interne WHERE fkStock = ? AND fkTransfertInterne = ?";
      try {
        Float sum = excludeLigneId != null
            ? jdbcTemplate.queryForObject(lignesSql, Float.class, fkStock, fkTransfertInterne, excludeLigneId)
            : jdbcTemplate.queryForObject(lignesSql, Float.class, fkStock, fkTransfertInterne);
        quantiteTotaleAutresLignes += sum != null ? sum : 0f;
      } catch (Exception e) {
        log.debug("Calcul quantité lignes existantes ignoré: {}", e.getMessage());
      }
    }

    float nouvelleQuantiteTotale = quantiteTotaleAutresLignes + quantity;
    if (nouvelleQuantiteTotale > stockDisponible) {
      throw new BusinessException(String.format(
          "La somme totale des quantités (%.2f) dépasse le stock disponible (%.2f)",
          nouvelleQuantiteTotale, stockDisponible));
    }
    if (quantity <= 0) {
      throw new BusinessException("La quantité doit être supérieure à 0");
    }
  }
}
