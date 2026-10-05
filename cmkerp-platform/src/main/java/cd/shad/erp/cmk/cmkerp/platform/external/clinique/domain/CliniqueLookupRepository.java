package cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain;

import java.util.List;
import java.util.Map;

/**
 * Accès lecture CLINIQUE (SQL Server). Aucune écriture / migration / DDL.
 */
public interface CliniqueLookupRepository {

  boolean isAvailable();

  long countMedicaments();

  List<Map<String, Object>> findMedicaments(int limit);

  List<Map<String, Object>> findTarifs(int limit);

  /** Recherche paginée {@code dbo.TPRODUIT}. */
  List<CliniqueProduit> searchProduits(String query, int offset, int limit);

  long countProduits(String query);
}
