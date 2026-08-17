package cd.shad.erp.cmk.cmkerp.platform.site.domain.repository;

import cd.shad.erp.cmk.cmkerp.platform.site.domain.model.Site;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'agrégat Site.
 *
 * <p>Cette interface définit le contrat de persistance pour les sites.
 * L'implémentation sera fournie dans la couche infrastructure.
 */
public interface SiteRepository {

    /**
     * Trouve un site par son ID.
     *
     * @param id l'ID du site
     * @return Optional contenant le site s'il existe
     */
    Optional<Site> findById(Long id);

    /**
     * Trouve un site par sa désignation.
     *
     * @param designation la désignation du site
     * @return Optional contenant le site s'il existe
     */
    Optional<Site> findByDesignation(String designation);

    /**
     * Récupère tous les sites.
     *
     * @return liste de tous les sites
     */
    List<Site> findAll();

    /**
     * Sauvegarde un nouveau site.
     *
     * @param site le site à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(Site site);

    /**
     * Met à jour un site existant.
     *
     * @param site le site à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(Site site);

    /**
     * Supprime un site par son ID.
     *
     * @param id l'ID du site à supprimer
     * @return le nombre de lignes affectées
     */
    int deleteById(Long id);
}

