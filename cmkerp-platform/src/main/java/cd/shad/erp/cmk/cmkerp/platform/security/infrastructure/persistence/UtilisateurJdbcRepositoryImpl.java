package cd.shad.erp.cmk.cmkerp.platform.security.infrastructure.persistence;

import cd.shad.erp.cmk.cmkerp.platform.security.domain.model.Utilisateur;
import cd.shad.erp.cmk.cmkerp.platform.security.domain.repository.UtilisateurRepository;
import cd.shad.erp.cmk.cmkerp.sharedkernel.repository.jdbc.AbstractJdbcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implémentation JDBC du repository Utilisateur.
 */
@Repository
@Slf4j
public class UtilisateurJdbcRepositoryImpl extends AbstractJdbcRepository implements UtilisateurRepository {

    public UtilisateurJdbcRepositoryImpl(
            @Qualifier("primaryJdbcTemplate") JdbcTemplate jdbcTemplate,
            @Qualifier("primaryNamedParameterJdbcTemplate") NamedParameterJdbcTemplate namedJdbcTemplate) {
        super(jdbcTemplate, namedJdbcTemplate);
    }

    /**
     * RowMapper optimisé pour MySQL 8.
     * Facebook-Grade : Projections explicites, pas de colonnes inutiles (datecreate, dateupdate, usercreateid, userupdateid).
     * 🎯 Mise à jour : Mappe la colonne 'genre' vers le champ 'sexe' du modèle.
     */
    private static final RowMapper<Utilisateur> UTILISATEUR_MAPPER = (rs, rowNum) -> {
        // 🎯 Mapper 'genre' vers 'sexe' (la colonne en base s'appelle 'genre', le champ Java 'sexe')
        String genre = null;
        try {
            genre = rs.getString("genre");
        } catch (java.sql.SQLException e) {
            // Si la colonne n'existe pas encore, on laisse null
        }

        return Utilisateur.builder()
                .id(rs.getLong("id"))
                .username(rs.getString("username"))
                .motDePasse(rs.getString("mot_de_passe"))
                .nom(rs.getString("nom"))
                .postnom(rs.getString("postnom"))
                .prenom(rs.getString("prenom"))
                .sexe(genre) // 🎯 Mapper 'genre' (colonne DB) vers 'sexe' (champ Java)
                .specialite(rs.getString("specialite"))
                .carted(rs.getString("carteid"))
                .locked(rs.getBoolean("locked"))
                .fkRole(rs.getLong("fkRole"))
                .initPassword(rs.getBoolean("initPassword"))
                .isLoginCard(rs.getBoolean("islogincard"))
                .dateCreate(null) // Non nécessaire pour le login
                .dateUpdate(null) // Non nécessaire pour le login
                .userCreatedId(null) // Non nécessaire pour le login
                .userUpdatedId(null) // Non nécessaire pour le login
                .build();
    };

    /**
     * Récupère un utilisateur par son ID.
     * Facebook-Grade : Projection explicite optimisée MySQL 8 (uniquement les colonnes nécessaires).
     */
    @Override
    public Optional<Utilisateur> findById(Long id) {
        // Projection explicite : meilleure performance MySQL 8, uniquement les colonnes nécessaires
        // 🎯 Mise à jour : Inclure la colonne 'genre' pour le mapping vers 'sexe'
        String sql = "SELECT id, username, mot_de_passe, nom, postnom, prenom, "
                + "specialite, carteid, locked, fkRole, initPassword, islogincard, genre "
                + "FROM utilisateurs WHERE id = ?";
        return queryForOptional(sql, UTILISATEUR_MAPPER, id);
    }

    /**
     * Récupère un utilisateur par son nom d'utilisateur.
     * Facebook-Grade : Projection explicite + index sur username pour performance MySQL 8.
     */
    @Override
    public Optional<Utilisateur> findByUsername(String username) {
        // Projection explicite : meilleure performance MySQL 8, uniquement les colonnes nécessaires
        // Note : Assurez-vous qu'un index existe sur 'username' pour optimiser cette requête
        // 🎯 Mise à jour : Inclure la colonne 'genre' pour le mapping vers 'sexe'
        String sql = "SELECT id, username, mot_de_passe, nom, postnom, prenom, "
                + "specialite, carteid, locked, fkRole, initPassword, islogincard, genre "
                + "FROM utilisateurs WHERE username = ? LIMIT 1";
        return queryForOptional(sql, UTILISATEUR_MAPPER, username);
    }

    /**
     * Récupère un utilisateur par son nom d'utilisateur avec le nom du rôle (pour le login).
     * Facebook-Grade : Jointure LEFT JOIN optimisée MySQL 8, une seule requête au lieu de deux.
     *
     * @param username le nom d'utilisateur
     * @return un tuple contenant l'utilisateur et le nom du rôle (peut être null)
     */
    @Override
    public Optional<Map.Entry<Utilisateur, String>> findByUsernameWithRole(String username) {
        // Jointure LEFT JOIN : récupère l'utilisateur ET le nom du rôle en une seule requête
        // Optimisation MySQL 8 : utilise les index sur username et fkRole
        // 🎯 Mise à jour : Inclure la colonne 'genre' pour le mapping vers 'sexe'
        String sql = "SELECT u.id, u.username, u.mot_de_passe, u.nom, u.postnom, u.prenom, "
                + "u.specialite, u.carteid, u.locked, u.fkRole, u.initPassword, u.islogincard, u.genre, "
                + "r.nom AS roleName "
                + "FROM utilisateurs u "
                + "LEFT JOIN roles r ON u.fkRole = r.id "
                + "WHERE u.username = ? LIMIT 1";

        try {
            List<Map.Entry<Utilisateur, String>> results = jdbcTemplate.query(sql, (rs, rowNum) -> {
                // 🎯 Mapper 'genre' vers 'sexe' (la colonne en base s'appelle 'genre', le champ Java 'sexe')
                String genre = null;
                try {
                    genre = rs.getString("genre");
                } catch (java.sql.SQLException e) {
                    // Si la colonne n'existe pas encore, on laisse null
                }

                Utilisateur utilisateur = Utilisateur.builder()
                        .id(rs.getLong("id"))
                        .username(rs.getString("username"))
                        .motDePasse(rs.getString("mot_de_passe"))
                        .nom(rs.getString("nom"))
                        .postnom(rs.getString("postnom"))
                        .prenom(rs.getString("prenom"))
                        .sexe(genre) // 🎯 Mapper 'genre' (colonne DB) vers 'sexe' (champ Java)
                        .specialite(rs.getString("specialite"))
                        .carted(rs.getString("carteid"))
                        .locked(rs.getBoolean("locked"))
                        .fkRole(rs.getLong("fkRole"))
                        .initPassword(rs.getBoolean("initPassword"))
                        .isLoginCard(rs.getBoolean("islogincard"))
                        .dateCreate(null)
                        .dateUpdate(null)
                        .userCreatedId(null)
                        .userUpdatedId(null)
                        .build();

                String roleName = rs.getString("roleName");

                return new AbstractMap.SimpleEntry<>(utilisateur, roleName);
            }, username);

            return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de l'utilisateur avec rôle pour username: {}", username, e);
            return Optional.empty();
        }
    }

    /**
     * Insère un nouvel utilisateur.
     * Facebook-Grade : INSERT optimisé avec gestion optionnelle de 'sexe'.
     */
    /**
     * Insère un nouvel utilisateur.
     * Facebook-Grade : INSERT optimisé, pas de colonnes inutiles (datecreate, usercreateid).
     */
    @Override
    public int save(Utilisateur utilisateur) {
        // INSERT optimisé : inclure mot_de_passe (obligatoire) et genre
        // 🎯 CRITICAL: Le mot de passe doit toujours être défini lors de la création
        String sql = "INSERT INTO utilisateurs (username, mot_de_passe, nom, postnom, prenom, specialite, carteid, fkRole, locked, initPassword, islogincard, genre) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        return update(sql,
            utilisateur.getUsername(),
            utilisateur.getMotDePasse(), // 🎯 CRITICAL: Mot de passe obligatoire
            utilisateur.getNom(),
            utilisateur.getPostnom(),
            utilisateur.getPrenom(),
            utilisateur.getSpecialite(),
            utilisateur.getCarted(),
            utilisateur.getFkRole(),
            utilisateur.getLocked() != null ? utilisateur.getLocked() : false,
            utilisateur.getInitPassword() != null ? utilisateur.getInitPassword() : false,
            utilisateur.getIsLoginCard() != null ? utilisateur.getIsLoginCard() : false,
            utilisateur.getSexe()); // 🎯 Mapper 'sexe' (champ Java) vers 'genre' (colonne DB)
    }

    /**
     * Met à jour un utilisateur existant.
     * Facebook-Grade : UPDATE optimisé, pas de colonnes inutiles (dateupdate, userupdateid).
     */
    @Override
    public int update(Utilisateur utilisateur) {
        // UPDATE optimisé : uniquement les colonnes nécessaires
        // 🎯 Mise à jour : Inclure la colonne 'genre' (mappée depuis 'sexe')
        String sql = "UPDATE utilisateurs SET username = ?, nom = ?, postnom = ?, prenom = ?, specialite = ?, "
                + "carteid = ?, fkRole = ?, locked = ?, initPassword = ?, islogincard = ?, genre = ? WHERE id = ?";

        return update(sql,
            utilisateur.getUsername(),
            utilisateur.getNom(),
            utilisateur.getPostnom(),
            utilisateur.getPrenom(),
            utilisateur.getSpecialite(),
            utilisateur.getCarted(),
            utilisateur.getFkRole(),
            utilisateur.getLocked(),
            utilisateur.getInitPassword(),
            utilisateur.getIsLoginCard(),
            utilisateur.getSexe(), // 🎯 Mapper 'sexe' (champ Java) vers 'genre' (colonne DB)
            utilisateur.getId());
    }

    @Override
    public int updatePassword(Long id, String motDePasseHash) {
        String sql = "UPDATE utilisateurs SET mot_de_passe = ?, dateupdate = NOW() WHERE id = ?";
        return update(sql, motDePasseHash, id);
    }

    @Override
    public int updateInitPassword(Long id, Boolean initPassword) {
        String sql = "UPDATE utilisateurs SET initPassword = ?, dateupdate = NOW() WHERE id = ?";
        return update(sql, initPassword != null ? initPassword : false, id);
    }

    /**
     * Récupère une liste paginée d'utilisateurs.
     * Facebook-Grade : Projection explicite + pagination optimisée MySQL 8.
     */
    @Override
    public List<Utilisateur> findAll(int offset, int limit) {
        // Projection explicite : meilleure performance MySQL 8, uniquement les colonnes nécessaires
        // ORDER BY id utilise l'index primaire (clustered index) pour de meilleures performances
        // 🎯 Mise à jour : Inclure la colonne 'genre' pour le mapping vers 'sexe'
        String sql = "SELECT id, username, mot_de_passe, nom, postnom, prenom, "
                + "specialite, carteid, locked, fkRole, initPassword, islogincard, genre "
                + "FROM utilisateurs ORDER BY id LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, UTILISATEUR_MAPPER, limit, offset);
    }

    /**
     * Récupère une liste paginée d'utilisateurs avec recherche.
     * Facebook-Grade : Recherche optimisée avec LIKE sur plusieurs colonnes.
     */
    @Override
    public List<Utilisateur> findAll(int offset, int limit, String searchTerm) {
        return findAll(offset, limit, searchTerm, null);
    }

    /**
     * Récupère une liste paginée d'utilisateurs avec recherche et filtre locked.
     * Facebook-Grade : Recherche optimisée avec LIKE sur plusieurs colonnes + filtre locked.
     */
    @Override
    public List<Utilisateur> findAll(int offset, int limit, String searchTerm, Boolean locked) {
        StringBuilder sql = new StringBuilder("SELECT id, username, mot_de_passe, nom, postnom, prenom, ")
                .append("specialite, carteid, locked, fkRole, initPassword, islogincard, genre ")
                .append("FROM utilisateurs WHERE 1=1");

        List<Object> params = new java.util.ArrayList<>();

        // Filtre de recherche
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchPattern = "%" + searchTerm.trim() + "%";
            sql.append(" AND (username LIKE ? OR nom LIKE ? OR postnom LIKE ? OR prenom LIKE ? OR specialite LIKE ?)");
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        // Filtre locked
        if (locked != null) {
            sql.append(" AND locked = ?");
            params.add(locked);
        }

        sql.append(" ORDER BY id LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), UTILISATEUR_MAPPER, params.toArray());
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM utilisateurs";
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    /**
     * Compte le nombre total d'utilisateurs avec recherche.
     */
    @Override
    public long count(String searchTerm) {
        return count(searchTerm, null);
    }

    /**
     * Compte le nombre total d'utilisateurs avec recherche et filtre locked.
     */
    @Override
    public long count(String searchTerm, Boolean locked) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM utilisateurs WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();

        // Filtre de recherche
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchPattern = "%" + searchTerm.trim() + "%";
            sql.append(" AND (username LIKE ? OR nom LIKE ? OR postnom LIKE ? OR prenom LIKE ? OR specialite LIKE ?)");
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
            params.add(searchPattern);
        }

        // Filtre locked
        if (locked != null) {
            sql.append(" AND locked = ?");
            params.add(locked);
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    @Override
    public Optional<String> findEmailByUsername(String username) {
        try {
            // Tente de récupérer l'email depuis la colonne 'email' si elle existe
            String sql = "SELECT email FROM utilisateurs WHERE username = ?";
            String email = jdbcTemplate.queryForObject(sql, String.class, username);
            return Optional.ofNullable(email).filter(e -> !e.trim().isEmpty());
        } catch (org.springframework.jdbc.BadSqlGrammarException e) {
            // La colonne 'email' n'existe pas dans la table
            log.debug("La colonne 'email' n'existe pas dans la table utilisateurs pour username: {}", username);
            return Optional.empty();
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            // Aucun résultat trouvé (utilisateur n'existe pas ou email est null)
            log.debug("Aucun email trouvé pour username: {}", username);
            return Optional.empty();
        } catch (Exception e) {
            // Autre erreur inattendue
            log.warn("Erreur lors de la récupération de l'email pour username: {}", username, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean existsByFkRole(Long roleId) {
        String sql = "SELECT COUNT(*) > 0 FROM utilisateurs WHERE fkRole = ?";
        Boolean exists = jdbcTemplate.queryForObject(sql, Boolean.class, roleId);
        return Boolean.TRUE.equals(exists);
    }

    @Override
    public int deleteById(Long id) {
        String sql = "DELETE FROM utilisateurs WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);
        log.debug("Suppression de l'utilisateur ID: {}, lignes affectées: {}", id, rowsAffected);
        return rowsAffected;
    }
}

