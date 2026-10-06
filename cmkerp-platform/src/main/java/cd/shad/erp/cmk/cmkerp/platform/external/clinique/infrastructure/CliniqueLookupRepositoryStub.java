package cd.shad.erp.cmk.cmkerp.platform.external.clinique.infrastructure;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueLookupRepository;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniquePrixInfo;
import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueProduit;

/**
 * Stub CLINIQUE quand l'URL SQL Server est vide.
 */
public class CliniqueLookupRepositoryStub implements CliniqueLookupRepository {

  @Override
  public boolean isAvailable() {
    return false;
  }

  @Override
  public long countMedicaments() {
    return 0L;
  }

  @Override
  public List<Map<String, Object>> findMedicaments(int limit) {
    return Collections.emptyList();
  }

  @Override
  public List<Map<String, Object>> findTarifs(int limit) {
    return Collections.emptyList();
  }

  @Override
  public List<CliniqueProduit> searchProduits(String query, int offset, int limit) {
    return Collections.emptyList();
  }

  @Override
  public long countProduits(String query) {
    return 0L;
  }

  @Override
  public List<CliniquePrixInfo> findPrixByCodes(List<String> codes) {
    return Collections.emptyList();
  }

  @Override
  public boolean updatePau(String code, double pau) {
    return false;
  }
}
