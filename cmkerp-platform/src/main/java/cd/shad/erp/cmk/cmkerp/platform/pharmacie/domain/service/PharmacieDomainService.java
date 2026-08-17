package cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.service;

import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model.Pharmacie;
import cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.repository.PharmacieRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Domain Service pour le domaine Pharmacie - Gestion des pharmacies.
 *
 * <p>Ce service contient la logique métier pure liée aux pharmacies qui ne peut pas
 * être encapsulée dans l'agrégat Pharmacie lui-même.
 *
 * <p>Responsabilités :
 * <ul>
 *   <li>Validation d'unicité du code immobilier par site (nécessite accès au repository)</li>
 *   <li>Règles métier complexes sur les pharmacies</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PharmacieDomainService {

    private final PharmacieRepository pharmacieRepository;

    /**
     * Valide que le code immobilier est unique pour un site donné.
     * Si le codeImmo est null ou vide, la validation est ignorée.
     *
     * @param codeImmo le code immobilier à valider
     * @param siteId l'ID du site
     * @param pharmacieIdExclu l'ID de la pharmacie à exclure de la vérification (pour les mises à jour, null pour création)
     * @throws BusinessException si une pharmacie avec ce code immobilier existe déjà pour ce site
     */
    public void validerCodeImmoUnique(String codeImmo, Long siteId, Long pharmacieIdExclu) {
        // Si pas de code immobilier, pas de validation d'unicité nécessaire
        if (codeImmo == null || codeImmo.trim().isEmpty()) {
            return;
        }

        Pharmacie.validerCodeImmo(codeImmo); // Valide d'abord le format

        pharmacieRepository.findByCodeImmo(codeImmo.trim())
            .ifPresent(existingPharmacie -> {
                // Vérifier que c'est bien pour le même site
                if (siteId != null && existingPharmacie.getFkSite() != null
                    && existingPharmacie.getFkSite().equals(siteId)) {
                    // Si on est en mode mise à jour, on ignore la pharmacie elle-même
                    if (pharmacieIdExclu == null || !existingPharmacie.getId().equals(pharmacieIdExclu)) {
                        throw new BusinessException(
                            String.format("Une pharmacie avec le code immobilier '%s' existe déjà pour ce site", codeImmo));
                    }
                }
            });
    }

    /**
     * Valide qu'une pharmacie peut être créée.
     * Vérifie que le code immobilier (s'il existe) est unique pour le site.
     *
     * @param codeImmo le code immobilier (peut être null)
     * @param siteId l'ID du site
     * @throws BusinessException si la pharmacie ne peut pas être créée
     */
    public void validerCreationPharmacie(String codeImmo, Long siteId) {
        validerCodeImmoUnique(codeImmo, siteId, null);
    }

    /**
     * Valide qu'une pharmacie peut être modifiée.
     * Vérifie que le nouveau code immobilier (s'il change) est unique pour le site.
     *
     * @param pharmacie la pharmacie existante
     * @param nouveauCodeImmo le nouveau code immobilier (peut être null si non modifié)
     * @throws BusinessException si la pharmacie ne peut pas être modifiée
     */
    public void validerModificationPharmacie(Pharmacie pharmacie, String nouveauCodeImmo) {
        if (nouveauCodeImmo != null && !nouveauCodeImmo.equals(pharmacie.getCodeimmo())) {
            validerCodeImmoUnique(nouveauCodeImmo, pharmacie.getFkSite(), pharmacie.getId());
        }
    }
}

