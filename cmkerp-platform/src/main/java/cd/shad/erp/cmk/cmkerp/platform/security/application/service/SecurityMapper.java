package cd.shad.erp.cmk.cmkerp.platform.security.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.DroitPharmacie;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Permission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions;

/**
 * Mapper central pour construire un UserPermissions
 * à partir des entités de sécurité (utilisateur, rôle, permissions, droits_pharmacies).
 */
public final class SecurityMapper {

    private SecurityMapper() {
        // utility class
    }

    public static UserPermissions mapToUserPermissions(
            Utilisateur utilisateur,
            Role role,
            List<Permission> permissions,
            List<DroitPharmacie> droitsPharmacies) {

        if (utilisateur == null) {
            return null;
        }

        Set<String> permissionCodes = permissions == null ? Collections.emptySet()
                : permissions.stream()
                        .map(Permission::getNom)       // IMPORTANT : ici tu dois être aligné avec `permissions.nom`
                        .collect(Collectors.toSet());

        Set<Long> pharmacieIds = droitsPharmacies == null ? Collections.emptySet()
                : droitsPharmacies.stream()
                        .map(DroitPharmacie::getFkPharmacie)
                        .collect(Collectors.toSet());

        return UserPermissions.builder()
                .userId(utilisateur.getId())
                .roleId(utilisateur.getFkRole())
                .username(utilisateur.getUsername())
                .nom(utilisateur.getNom())
                .postnom(utilisateur.getPostnom())
                .prenom(utilisateur.getPrenom())
                .sexe(utilisateur.getSexe())
                .specialite(utilisateur.getSpecialite())
                .roleName(role != null ? role.getNom() : null)
                .locked(Boolean.TRUE.equals(utilisateur.getLocked()))
                .initPassword(Boolean.TRUE.equals(utilisateur.getInitPassword()))
                // siteId : si un jour tu l'ajoutes dans la table ou une vue, tu remplis ici
                .siteId(null)
                .permissions(permissionCodes)
                .pharmacieIds(pharmacieIds)
                .build();
    }

    /**
     * Version minimale : uniquement les infos de base utilisateur + rôle,
     * sans charger les permissions/droits. Pratique pour des checks rapides.
     */
    public static UserPermissions mapMinimal(Utilisateur utilisateur) {
        if (utilisateur == null) {
            return null;
        }

        return UserPermissions.builder()
                .userId(utilisateur.getId())
                .roleId(utilisateur.getFkRole())
                .username(utilisateur.getUsername())
                .nom(utilisateur.getNom())
                .postnom(utilisateur.getPostnom())
                .prenom(utilisateur.getPrenom())
                .sexe(utilisateur.getSexe())
                .specialite(utilisateur.getSpecialite())
                .roleName(null) // Nécessite le chargement du Role
                .locked(Boolean.TRUE.equals(utilisateur.getLocked()))
                .initPassword(Boolean.TRUE.equals(utilisateur.getInitPassword()))
                .build(); // sets vides par défaut grâce à @Builder.Default
    }
}

