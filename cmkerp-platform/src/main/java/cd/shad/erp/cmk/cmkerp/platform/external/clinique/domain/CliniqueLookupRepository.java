package cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain;

import java.util.List;
import java.util.Map;

/**
 * Accès CLINIQUE (SQL Server). Pas de DROP / DELETE / DDL. UPDATE limité à {@code TSTOCK.PAU}.
 */
public interface CliniqueLookupRepository {

  boolean isAvailable();

  long countMedicaments();

  List<Map<String, Object>> findMedicaments(int limit);

  List<Map<String, Object>> findTarifs(int limit);

  /** Recherche paginée {@code dbo.TPRODUIT}. */
  List<CliniqueProduit> searchProduits(String query, int offset, int limit);

  long countProduits(String query);

  /** Prix / stock TSTOCK pour une liste de codes TPRODUIT. */
  List<CliniquePrixInfo> findPrixByCodes(List<String> codes);

  /**
   * Met à jour {@code dbo.TSTOCK.PAU} uniquement (pas d'autre écriture).
   *
   * @return true si une ligne a été mise à jour ou insérée
   */
  boolean updatePau(String code, double pau);
}
