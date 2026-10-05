package cd.shad.erp.cmk.cmkerp.platform.external.clinique.infrastructure;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain.CliniqueLookupRepository;

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
}
