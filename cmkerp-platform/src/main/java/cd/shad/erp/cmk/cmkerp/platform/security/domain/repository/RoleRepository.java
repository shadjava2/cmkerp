package cd.shad.erp.cmk.cmkerp.platform.security.domain.repository;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Role;
import java.util.List;
import java.util.Optional;

/**
 * Interface de repository pour l'agrégat Role.
 *
 * <p>Cette interface définit le contrat de persistance pour les rôles.
 * L'implémentation sera fournie dans la couche infrastructure.
 */
public interface RoleRepository {

    /**
     * Trouve un rôle par son ID.
     *
     * @param id l'ID du rôle
     * @return Optional contenant le rôle s'il existe
     */
    Optional<Role> findById(Long id);

    /**
     * Trouve un rôle par son nom.
     *
     * @param nom le nom du rôle
     * @return Optional contenant le rôle s'il existe
     */
    Optional<Role> findByNom(String nom);

    /**
     * Récupère tous les rôles.
     *
     * @return liste de tous les rôles
     */
    List<Role> findAll();

    /**
     * Sauvegarde un nouveau rôle.
     *
     * @param role le rôle à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(Role role);

    /**
     * Met à jour un rôle existant.
     *
     * @param role le rôle à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(Role role);

    /**
     * Supprime un rôle par son ID.
     *
     * @param id l'ID du rôle à supprimer
     * @return le nombre de lignes affectées
     */
    int deleteById(Long id);
}

