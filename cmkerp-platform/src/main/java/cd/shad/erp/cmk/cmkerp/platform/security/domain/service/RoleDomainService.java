package cd.shad.erp.cmk.cmkerp.platform.security.domain.service;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.RolePermission;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RolePermissionRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.RoleRepository;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.exception.BusinessException;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.SystemRoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleDomainService {

    private final RoleRepository roleRepository;
    private final UtilisateurRepository utilisateurRepository;

    public void validerNomUnique(String nom, Long roleIdExclu) {
        Role.validerNom(nom);

        if (SystemRoleConstants.isAdminRoleName(nom)
            && (roleIdExclu == null || !SystemRoleConstants.isAdminRoleId(roleIdExclu))) {
            throw new BusinessException("Le nom ADMIN est réservé au rôle système protégé");
        }

        roleRepository.findByNom(nom.trim())
            .ifPresent(existingRole -> {
                if (roleIdExclu == null || !existingRole.getId().equals(roleIdExclu)) {
                    throw new BusinessException("Un rôle avec ce nom existe déjà");
                }
            });
    }

    public void validerRoleNonSysteme(Long roleId) {
        if (SystemRoleConstants.isAdminRoleId(roleId)) {
            throw new BusinessException(
                "Le rôle système ADMIN est protégé et ne peut pas être modifié depuis l'application");
        }
    }

    public void validerSuppressionRole(Long roleId) {
        if (roleId == null) {
            throw new IllegalArgumentException("L'ID du rôle ne peut pas être null");
        }

        validerRoleNonSysteme(roleId);

        if (utilisateurRepository.existsByFkRole(roleId)) {
            throw new BusinessException("Impossible de supprimer ce rôle car il est utilisé par au moins un utilisateur");
        }
    }

    public Role chargerPermissions(Role role, RolePermissionRepository rolePermissionRepository) {
        if (role == null || role.getId() == null) {
            return role;
        }

        List<RolePermission> rolePermissions = rolePermissionRepository.findByRole(role.getId());

        Set<Long> permissionIds = rolePermissions.stream()
            .map(RolePermission::getFkPermission)
            .collect(Collectors.toSet());

        role.setPermissionIds(permissionIds);
        return role;
    }
}

