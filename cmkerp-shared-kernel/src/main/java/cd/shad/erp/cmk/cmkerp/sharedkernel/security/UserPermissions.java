package cd.shad.erp.cmk.cmkerp.sharedkernel.security;

import java.util.HashSet;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Snapshot du contexte de sécurité d'un utilisateur connecté.
 * Utilisé par les modules gateway / platform sans dépendre de Spring Security.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissions {

    /** ID technique de l'utilisateur */
    private Long userId;

    /** ID du rôle principal */
    private Long roleId;

    /** Login */
    private String username;

    /** Nom de famille */
    private String nom;

    /** Postnom */
    private String postnom;

    /** Prénom */
    private String prenom;

    /** Sexe ("M" pour Masculin, "F" pour Féminin) */
    private String sexe;

    /** Spécialité */
    private String specialite;

    /** Nom du rôle (dérivé depuis Role.nom) */
    private String roleName;

    /** Compte verrouillé ? */
    private boolean locked;

    /** Doit changer le mot de passe initial ? */
    private boolean initPassword;

    /** Site principal (optionnel) */
    private Long siteId;

    /**
     * Codes de permissions (valeurs de la colonne `permissions.nom`).
     * Ex : "USER_VIEW", "STOCK_MANAGE", ...
     */
    @Builder.Default
    private Set<String> permissions = new HashSet<>();

    /**
     * Liste des pharmacies accessibles (droits_pharmacies.fkPharmacie).
     */
    @Builder.Default
    private Set<Long> pharmacieIds = new HashSet<>();

    /** Vérifie si l'utilisateur possède un code permission. */
    public boolean hasPermission(String code) {
        return permissions.contains(code);
    }

    /** Vérifie si l'utilisateur peut accéder à une pharmacie. */
    public boolean canAccessPharmacy(Long pharmacieId) {
        return pharmacieIds.contains(pharmacieId);
    }
}
