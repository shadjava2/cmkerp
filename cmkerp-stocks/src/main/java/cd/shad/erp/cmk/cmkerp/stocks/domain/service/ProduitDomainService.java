package cd.shad.erp.cmk.cmkerp.stocks.domain.service;

import cd.shad.erp.cmk.cmkerp.stocks.domain.model.Produit;
import cd.shad.erp.cmk.cmkerp.stocks.domain.repository.ProduitRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Domain Service pour le domaine Stocks - Gestion des produits.
 *
 * <p>Ce service contient la logique métier pure liée aux produits qui ne peut pas
 * être encapsulée dans l'agrégat Produit lui-même.
 *
 * <p>Responsabilités :
 * <ul>
 *   <li>Validation d'unicité du code-barres (nécessite accès au repository)</li>
 *   <li>Règles métier complexes sur les produits</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class ProduitDomainService {

    private final ProduitRepository produitRepository;

    /**
     * Valide que le code-barres est unique.
     * Si le codebarre est null ou vide, la validation est ignorée.
     *
     * @param codebarre le code-barres à valider
     * @param produitIdExclu l'ID du produit à exclure de la vérification (pour les mises à jour, null pour création)
     * @throws BusinessException si un produit avec ce code-barres existe déjà
     */
    public void validerCodebarreUnique(String codebarre, Long produitIdExclu) {
        // Si pas de code-barres, pas de validation d'unicité nécessaire
        if (codebarre == null || codebarre.trim().isEmpty()) {
            return;
        }

        Produit.validerCodebarre(codebarre); // Valide d'abord le format

        produitRepository.findByCodebarre(codebarre.trim())
            .ifPresent(existingProduit -> {
                // Si on est en mode mise à jour, on ignore le produit lui-même
                if (produitIdExclu == null || !existingProduit.getId().equals(produitIdExclu)) {
                    throw new BusinessException(
                        String.format("Un produit avec le code-barres '%s' existe déjà", codebarre));
                }
            });
    }

    /**
     * Valide qu'un produit peut être créé.
     * Vérifie que le code-barres (s'il existe) est unique.
     *
     * @param codebarre le code-barres (peut être null)
     * @throws BusinessException si le produit ne peut pas être créé
     */
    public void validerCreationProduit(String codebarre) {
        validerCodebarreUnique(codebarre, null);
    }

    /**
     * Valide qu'un produit peut être modifié.
     * Vérifie que le nouveau code-barres (s'il change) est unique.
     *
     * @param produit le produit existant
     * @param nouveauCodebarre le nouveau code-barres (peut être null si non modifié)
     * @throws BusinessException si le produit ne peut pas être modifié
     */
    public void validerModificationProduit(Produit produit, String nouveauCodebarre) {
        if (nouveauCodebarre != null && !nouveauCodebarre.equals(produit.getCodebarre())) {
            validerCodebarreUnique(nouveauCodebarre, produit.getId());
        }
    }
}

