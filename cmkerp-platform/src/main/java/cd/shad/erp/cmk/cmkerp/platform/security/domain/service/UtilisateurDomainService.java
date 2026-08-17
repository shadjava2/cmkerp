package cd.shad.erp.cmk.cmkerp.platform.security.domain.service;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Domain Service pour le domaine Security - Gestion des utilisateurs.
 *
 * <p>Ce service contient la logique métier pure liée aux utilisateurs qui ne peut pas
 * être encapsulée dans l'agrégat Utilisateur lui-même.
 *
 * <p>Responsabilités :
 * <ul>
 *   <li>Validation d'unicité du username (nécessite accès au repository)</li>
 *   <li>Règles métier complexes sur les utilisateurs</li>
 *   <li>Validation de changement de mot de passe</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UtilisateurDomainService {

    private final UtilisateurRepository utilisateurRepository;

    /**
     * Valide que le username est unique.
     *
     * @param username le username à valider
     * @param utilisateurIdExclu l'ID de l'utilisateur à exclure de la vérification (pour les mises à jour, null pour création)
     * @throws BusinessException si un utilisateur avec ce username existe déjà
     */
    public void validerUsernameUnique(String username, Long utilisateurIdExclu) {
        Utilisateur.validerUsername(username); // Valide d'abord le format

        utilisateurRepository.findByUsername(username.trim())
            .ifPresent(existingUser -> {
                // Si on est en mode mise à jour, on ignore l'utilisateur lui-même
                if (utilisateurIdExclu == null || !existingUser.getId().equals(utilisateurIdExclu)) {
                    throw new BusinessException("Un utilisateur avec ce nom d'utilisateur existe déjà");
                }
            });
    }

    /**
     * Valide qu'un utilisateur peut être créé.
     * Vérifie que le username est unique.
     *
     * @param username le username de l'utilisateur
     * @throws BusinessException si l'utilisateur ne peut pas être créé
     */
    public void validerCreationUtilisateur(String username) {
        validerUsernameUnique(username, null);
    }

    /**
     * Valide qu'un utilisateur peut être modifié.
     * Vérifie que le nouveau username (s'il change) est unique.
     *
     * @param utilisateur l'utilisateur existant
     * @param nouveauUsername le nouveau username (peut être null si non modifié)
     * @throws BusinessException si l'utilisateur ne peut pas être modifié
     */
    public void validerModificationUtilisateur(Utilisateur utilisateur, String nouveauUsername) {
        if (nouveauUsername != null && !nouveauUsername.equals(utilisateur.getUsername())) {
            validerUsernameUnique(nouveauUsername, utilisateur.getId());
        }
    }

    /**
     * Valide qu'un utilisateur peut changer son mot de passe.
     * Vérifie que le compte n'est pas verrouillé.
     *
     * @param utilisateur l'utilisateur
     * @throws BusinessException si le mot de passe ne peut pas être changé
     */
    public void validerChangementMotDePasse(Utilisateur utilisateur) {
        if (utilisateur.estVerrouille()) {
            throw new BusinessException("Impossible de changer le mot de passe d'un compte verrouillé");
        }
    }
}

