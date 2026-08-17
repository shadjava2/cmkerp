package cd.shad.erp.cmk.cmkerp.platform.security.domain.repository;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Permission;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'entité Permission.
 *
 * <p>Cette interface définit le contrat de persistance pour les permissions.
 * L'implémentation sera fournie dans la couche infrastructure.
 */
public interface PermissionRepository {

    /**
     * Trouve une permission par son ID.
     *
     * @param id l'ID de la permission
     * @return Optional contenant la permission s'elle existe
     */
    Optional<Permission> findById(Long id);

    /**
     * Trouve une permission par son nom.
     *
     * @param nom le nom de la permission
     * @return Optional contenant la permission s'elle existe
     */
    Optional<Permission> findByNom(String nom);

    /**
     * Récupère toutes les permissions.
     *
     * @return liste de toutes les permissions
     */
    List<Permission> findAll();
}

