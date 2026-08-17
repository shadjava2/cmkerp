package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

/**
 * Segmentation des produits — activité mesurée sur le mois en cours uniquement.
 */
public enum StockProductCategory {
  /** Au moins une entrée ou sortie sur le mois en cours */
  AVEC_MOUVEMENT,
  /** Stock disponible, aucune entrée/sortie ce mois */
  STOCK_SANS_MOUVEMENT,
  /** Rupture ou sous seuil, aucune entrée/sortie ce mois */
  RUPTURE_SANS_MOUVEMENT
}
