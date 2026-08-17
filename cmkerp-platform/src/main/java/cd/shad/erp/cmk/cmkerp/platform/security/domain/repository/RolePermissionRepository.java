package cd.shad.erp.cmk.cmkerp.platform.security.domain.repository;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.RolePermission;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'entité RolePermission.
 *
 * <p>Cette interface définit le contrat de persistance pour les associations
 * entre rôles et permissions. L'implémentation sera fournie dans la couche infrastructure.
 */
public interface RolePermissionRepository {

    /**
     * Trouve une association par son ID.
     *
     * @param id l'ID de l'association
     * @return Optional contenant l'association si elle existe
     */
    Optional<RolePermission> findById(Long id);

    /**
     * Trouve toutes les permissions associées à un rôle.
     *
     * @param roleId l'ID du rôle
     * @return liste des associations
     */
    List<RolePermission> findByRole(Long roleId);

    /**
     * Trouve toutes les associations pour une permission donnée.
     *
     * @param permissionId l'ID de la permission
     * @return liste des associations
     */
    List<RolePermission> findByPermission(Long permissionId);

    /**
     * Trouve une association spécifique entre un rôle et une permission.
     *
     * @param roleId l'ID du rôle
     * @param permissionId l'ID de la permission
     * @return Optional contenant l'association si elle existe
     */
    Optional<RolePermission> findByRoleAndPermission(Long roleId, Long permissionId);

    /**
     * Sauvegarde une nouvelle association.
     *
     * @param rolePermission l'association à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(RolePermission rolePermission);

    /**
     * Supprime une association par son ID.
     *
     * @param id l'ID de l'association à supprimer
     * @return le nombre de lignes affectées
     */
    int deleteById(Long id);

    /**
     * Supprime une association entre un rôle et une permission.
     *
     * @param roleId l'ID du rôle
     * @param permissionId l'ID de la permission
     * @return le nombre de lignes affectées
     */
    int deleteByRoleAndPermission(Long roleId, Long permissionId);
}

