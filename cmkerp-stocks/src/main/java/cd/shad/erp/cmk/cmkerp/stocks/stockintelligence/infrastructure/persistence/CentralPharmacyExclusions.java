package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.infrastructure.persistence;

import java.util.List;

/**
 * Pharmacies centrales exclues — liste vide : toutes les centrales sont visibles.
 */
public final class CentralPharmacyExclusions {

  public static final List<Long> EXCLUDED_IDS = List.of();

  /** Fragment SQL vide (aucune exclusion). */
  public static final String SQL_NOT_IN = "";

  private CentralPharmacyExclusions() {}
}
