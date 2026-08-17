package cd.shad.erp.cmk.cmkerp.platform.site.domain.service;

import cd.shad.erp.cmk.cmkerp.platform.site.domain.model.Site;
import cd.shad.erp.cmk.cmkerp.platform.site.domain.repository.SiteRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Domain Service pour le domaine Site - Gestion des sites.
 *
 * <p>Ce service contient la logique métier pure liée aux sites qui ne peut pas
 * être encapsulée dans l'agrégat Site lui-même.
 *
 * <p>Responsabilités :
 * <ul>
 *   <li>Validation d'unicité de la désignation (nécessite accès au repository)</li>
 *   <li>Règles métier complexes sur les sites</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SiteDomainService {

    private final SiteRepository siteRepository;

    /**
     * Valide que la désignation du site est unique.
     *
     * @param designation la désignation à valider
     * @param siteIdExclu l'ID du site à exclure de la vérification (pour les mises à jour, null pour création)
     * @throws BusinessException si un site avec cette désignation existe déjà
     */
    public void validerDesignationUnique(String designation, Long siteIdExclu) {
        Site.validerDesignation(designation); // Valide d'abord le format

        siteRepository.findByDesignation(designation.trim())
            .ifPresent(existingSite -> {
                // Si on est en mode mise à jour, on ignore le site lui-même
                if (siteIdExclu == null || !existingSite.getId().equals(siteIdExclu)) {
                    throw new BusinessException("Un site avec cette désignation existe déjà");
                }
            });
    }

    /**
     * Valide qu'un site peut être créé.
     * Vérifie que la désignation est unique.
     *
     * @param designation la désignation du site
     * @throws BusinessException si le site ne peut pas être créé
     */
    public void validerCreationSite(String designation) {
        validerDesignationUnique(designation, null);
    }

    /**
     * Valide qu'un site peut être modifié.
     * Vérifie que la nouvelle désignation (si elle change) est unique.
     *
     * @param site le site existant
     * @param nouvelleDesignation la nouvelle désignation (peut être null si non modifiée)
     * @throws BusinessException si le site ne peut pas être modifié
     */
    public void validerModificationSite(Site site, String nouvelleDesignation) {
        if (nouvelleDesignation != null && !nouvelleDesignation.equals(site.getDesignation())) {
            validerDesignationUnique(nouvelleDesignation, site.getId());
        }
    }
}

