package cd.shad.erp.cmk.cmkerp.platform.security.domain.repository;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.DroitPharmacie;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'entité DroitPharmacie.
 */
public interface DroitPharmacieRepository {

    /**
     * Trouve un droit par son ID.
     */
    Optional<DroitPharmacie> findById(Long id);

    /**
     * Trouve tous les droits d'accès d'un utilisateur.
     */
    List<DroitPharmacie> findByUtilisateur(Long utilisateurId);

    /**
     * Trouve tous les droits d'accès pour une pharmacie.
     */
    List<DroitPharmacie> findByPharmacie(Long pharmacieId);

    /**
     * Trouve un droit spécifique pour un utilisateur et une pharmacie.
     */
    Optional<DroitPharmacie> findByUtilisateurAndPharmacie(Long utilisateurId, Long pharmacieId);

    /**
     * Sauvegarde un nouveau droit.
     */
    int save(DroitPharmacie droitPharmacie);

    /**
     * Supprime un droit par son ID.
     */
    int deleteById(Long id);

    /**
     * Supprime un droit pour un utilisateur et une pharmacie.
     */
    int deleteByUtilisateurAndPharmacie(Long utilisateurId, Long pharmacieId);
}

