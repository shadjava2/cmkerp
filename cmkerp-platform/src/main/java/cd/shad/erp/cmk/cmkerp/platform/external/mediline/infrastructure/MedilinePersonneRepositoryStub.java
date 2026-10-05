package cd.shad.erp.cmk.cmkerp.platform.external.mediline.infrastructure;

import java.util.List;
import java.util.Optional;

import cd.shad.erp.cmk.cmkerp.platform.external.mediline.domain.MedilinePersonne;
import cd.shad.erp.cmk.cmkerp.platform.external.mediline.domain.MedilinePersonneRepository;

/**
 * Stub Mediline quand l'URL secondaire est vide.
 */
public class MedilinePersonneRepositoryStub implements MedilinePersonneRepository {

  @Override
  public Optional<MedilinePersonne> findByCode(String code) {
    return Optional.empty();
  }

  @Override
  public List<MedilinePersonne> searchByNom(String nomLike, int limit) {
    return List.of();
  }

  @Override
  public boolean isAvailable() {
    return false;
  }
}
