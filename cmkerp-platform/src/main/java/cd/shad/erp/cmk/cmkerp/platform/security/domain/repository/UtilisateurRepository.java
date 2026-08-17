package cd.shad.erp.cmk.cmkerp.platform.security.domain.repository;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import java.util.Map;
import java.util.Optional;

/**
 * Interface de repository pour l'agrégat Utilisateur.
 *
 * <p>Cette interface définit le contrat de persistance pour les utilisateurs.
 * L'implémentation sera fournie dans la couche infrastructure.
 */
public interface UtilisateurRepository {

    /**
     * Trouve un utilisateur par son ID.
     *
     * @param id l'ID de l'utilisateur
     * @return Optional contenant l'utilisateur s'il existe
     */
    Optional<Utilisateur> findById(Long id);

    /**
     * Trouve un utilisateur par son nom d'utilisateur (username).
     *
     * @param username le nom d'utilisateur
     * @return Optional contenant l'utilisateur s'il existe
     */
    Optional<Utilisateur> findByUsername(String username);

    /**
     * Trouve un utilisateur par son nom d'utilisateur avec le nom du rôle (pour le login).
     * Facebook-Grade : Jointure LEFT JOIN optimisée, une seule requête au lieu de deux.
     *
     * @param username le nom d'utilisateur
     * @return Optional contenant un Map.Entry avec l'utilisateur et le nom du rôle (peut être null)
     */
    Optional<Map.Entry<Utilisateur, String>> findByUsernameWithRole(String username);

    /**
     * Sauvegarde un nouvel utilisateur.
     *
     * @param utilisateur l'utilisateur à sauvegarder
     * @return le nombre de lignes affectées
     */
    int save(Utilisateur utilisateur);

    /**
     * Met à jour un utilisateur existant.
     *
     * @param utilisateur l'utilisateur à mettre à jour
     * @return le nombre de lignes affectées
     */
    int update(Utilisateur utilisateur);

    /**
     * Met à jour le mot de passe d'un utilisateur.
     *
     * @param id l'ID de l'utilisateur
     * @param motDePasseHash le hash du nouveau mot de passe
     * @return le nombre de lignes affectées
     */
    int updatePassword(Long id, String motDePasseHash);

    /**
     * Met à jour uniquement le champ initPassword d'un utilisateur.
     *
     * @param id l'ID de l'utilisateur
     * @param initPassword la nouvelle valeur de initPassword
     * @return le nombre de lignes affectées
     */
    int updateInitPassword(Long id, Boolean initPassword);

    /**
     * Récupère tous les utilisateurs avec pagination.
     *
     * @param offset l'offset (nombre d'éléments à sauter)
     * @param limit le nombre maximum d'éléments à retourner
     * @return la liste des utilisateurs
     */
    java.util.List<Utilisateur> findAll(int offset, int limit);

    /**
     * Récupère tous les utilisateurs avec pagination et recherche.
     *
     * @param offset l'offset (nombre d'éléments à sauter)
     * @param limit le nombre maximum d'éléments à retourner
     * @param searchTerm terme de recherche (username, nom, postnom, prenom, specialite)
     * @return la liste des utilisateurs
     */
    java.util.List<Utilisateur> findAll(int offset, int limit, String searchTerm);

    /**
     * Récupère tous les utilisateurs avec pagination, recherche et filtre locked.
     *
     * @param offset l'offset (nombre d'éléments à sauter)
     * @param limit le nombre maximum d'éléments à retourner
     * @param searchTerm terme de recherche (username, nom, postnom, prenom, specialite)
     * @param locked filtre sur le statut verrouillé (true = verrouillé, false = non verrouillé, null = tous)
     * @return la liste des utilisateurs
     */
    java.util.List<Utilisateur> findAll(int offset, int limit, String searchTerm, Boolean locked);

    /**
     * Compte le nombre total d'utilisateurs.
     *
     * @return le nombre total d'utilisateurs
     */
    long count();

    /**
     * Compte le nombre total d'utilisateurs avec recherche.
     *
     * @param searchTerm terme de recherche (username, nom, postnom, prenom, specialite)
     * @return le nombre total d'utilisateurs correspondant à la recherche
     */
    long count(String searchTerm);

    /**
     * Compte le nombre total d'utilisateurs avec recherche et filtre locked.
     *
     * @param searchTerm terme de recherche (username, nom, postnom, prenom, specialite)
     * @param locked filtre sur le statut verrouillé (true = verrouillé, false = non verrouillé, null = tous)
     * @return le nombre total d'utilisateurs correspondant aux critères
     */
    long count(String searchTerm, Boolean locked);

    /**
     * Récupère l'adresse email d'un utilisateur par son nom d'utilisateur.
     * Retourne Optional.empty() si la colonne email n'existe pas dans la table
     * ou si l'email n'est pas défini.
     *
     * @param username le nom d'utilisateur
     * @return Optional contenant l'email s'il existe, Optional.empty() sinon
     */
    Optional<String> findEmailByUsername(String username);

    /**
     * Vérifie si au moins un utilisateur utilise le rôle spécifié.
     *
     * @param roleId l'ID du rôle à vérifier
     * @return true si au moins un utilisateur utilise ce rôle, false sinon
     */
    boolean existsByFkRole(Long roleId);

    /**
     * Supprime un utilisateur par son ID.
     *
     * @param id l'ID de l'utilisateur à supprimer
     * @return le nombre de lignes affectées (1 si supprimé, 0 si non trouvé)
     */
    int deleteById(Long id);
}

