package cd.shad.erp.cmk.cmkerp.platform.approvisionnements.domain.repository;

import cd.shad.erp.cmk.cmkerp.platform.approvisionnements.domain.model.EchangeDevise;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour les échanges de devise.
 */
public interface EchangeDeviseRepository {

    /**
     * Trouve un échange de devise par son ID.
     */
    Optional<EchangeDevise> findById(Long id);

    /**
     * Récupère tous les échanges de devise (pour combo).
     */
    List<EchangeDevise> findAll();
}



