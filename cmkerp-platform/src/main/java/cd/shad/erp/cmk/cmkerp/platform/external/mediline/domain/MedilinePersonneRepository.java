package cd.shad.erp.cmk.cmkerp.platform.external.mediline.domain;

import java.util.List;
import java.util.Optional;

/**
 * Accès lecture Mediline ({@code t_personne}). Aucune écriture / migration.
 */
public interface MedilinePersonneRepository {

  Optional<MedilinePersonne> findByCode(String code);

  List<MedilinePersonne> searchByNom(String nomLike, int limit);

  boolean isAvailable();
}
