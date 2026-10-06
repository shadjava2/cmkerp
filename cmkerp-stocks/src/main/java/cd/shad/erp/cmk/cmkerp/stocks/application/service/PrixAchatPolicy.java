package cd.shad.erp.cmk.cmkerp.stocks.application.service;

import java.math.BigDecimal;

/**
 * Règle prix d'achat CMKERP / PAU CLINIQUE :
 * <ul>
 *   <li>stock &gt; 0 et nouveau prix plus bas → on conserve le prix élevé du stock</li>
 *   <li>nouveau prix plus élevé → on actualise</li>
 *   <li>stock = 0 → on prend le nouveau prix (même plus bas)</li>
 * </ul>
 */
public final class PrixAchatPolicy {

  private PrixAchatPolicy() {}

  public static BigDecimal resolve(BigDecimal current, BigDecimal incoming, double stockOnHand) {
    if (incoming == null) {
      return current;
    }
    if (current == null) {
      return incoming;
    }
    if (stockOnHand > 0.0001d) {
      return incoming.compareTo(current) > 0 ? incoming : current;
    }
    return incoming;
  }
}
