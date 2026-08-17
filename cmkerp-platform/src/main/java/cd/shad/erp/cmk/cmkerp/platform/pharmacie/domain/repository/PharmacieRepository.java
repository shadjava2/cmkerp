package cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.repository;

import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model.Pharmacie;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'agrégat Pharmacie.
 *
 * <p>Cette interface définit le contrat de persistance pour les pharmacies.
 * L'implémentation sera fournie dans la couche infrastructure.
 */
public interface PharmacieRepository {

    /**
     * Trouve une pharmacie par son ID.
     *
     * @param id l'ID de la pharmacie
     * @return Optional contenant la pharmacie si elle existe
     */
    Optional<Pharmacie> findById(Long id);

    /**
     * Trouve une pharmacie par son code immobilier.
     *
     * @param codeImmo le code immobilier
     * @return Optional contenant la pharmacie si elle existe
     */
    Optional<Pharmacie> findByCodeImmo(String codeImmo);

    /**
     * Trouve toutes les pharmacies associées à un site.
     *
     * @param siteId l'ID du site
     * @return liste des pharmacies
     */
    List<Pharmacie> findBySite(Long siteId);

    /**
     * Récupère toutes les pharmacies.
     *
     * @return liste de toutes les pharmacies
     */
    List<Pharmacie> findAll();

    /**
     * Sauvegarde une nouvelle pharmacie.
     *
     * @param pharmacie la pharmacie à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(Pharmacie pharmacie);

    /**
     * Met à jour une pharmacie existante.
     *
     * @param pharmacie la pharmacie à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(Pharmacie pharmacie);

    /**
     * Supprime une pharmacie par son ID.
     *
     * @param id l'ID de la pharmacie à supprimer
     * @return le nombre de lignes affectées
     */
    int deleteById(Long id);
}

