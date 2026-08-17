package cd.shad.erp.cmk.cmkerp.platform.security.domain.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Agrégat Root pour le domaine Security - Utilisateur.
 *
 * <p>Un Utilisateur est un agrégat qui encapsule :
 * <ul>
 *   <li>Les informations de l'utilisateur (nom, username, mot de passe)</li>
 *   <li>Le statut de verrouillage</li>
 *   <li>Le rôle associé</li>
 *   <li>Les invariants métier (username unique, mot de passe non vide si locked=false)</li>
 * </ul>
 *
 * <p>Méthodes métier disponibles :
 * <ul>
 *   <li>{@link #changerMotDePasse(String, String)} - Changer le mot de passe avec validation</li>
 *   <li>{@link #verrouiller()} - Verrouiller le compte</li>
 *   <li>{@link #deverrouiller()} - Déverrouiller le compte</li>
 *   <li>{@link #estVerrouille()} - Vérifier si le compte est verrouillé</li>
 *   <li>{@link #validerUsername(String)} - Valider que le username respecte les règles</li>
 * </ul>
 */
@Entity
@Table(name = "utilisateurs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String specialite;

    private String nom;
    private String postnom;
    private String prenom;
    private String sexe; // "M" pour Masculin, "F" pour Féminin

    private String username;

    @Column(name = "mot_de_passe")
    private String motDePasse;

    @Column(name = "locked")
    private Boolean locked;           // tinyint(1)

    @Column(name = "fkRole")
    private Long fkRole;              // pas de @ManyToOne, on garde l'id

    @Column(name = "initPassword")
    private Boolean initPassword;     // false = doit changer le mot de passe (première connexion), true = déjà changé

    private String carted;

    @Column(name = "islogincard")
    private Boolean isLoginCard;

    @Column(name = "datecreate")
    private LocalDateTime dateCreate;

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    @Column(name = "userupdateid")
    private Long userUpdatedId;

    // ============================================
    // MÉTHODES MÉTIER - Règles de l'agrégat
    // ============================================

    @Transient
    private Role fkrole;

    /**
     * Valide que le username respecte les règles métier.
     *
     * @param username le username à valider
     * @throws IllegalArgumentException si le username est invalide
     */
    public static void validerUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom d'utilisateur ne peut pas être vide");
        }
        if (username.length() < 3) {
            throw new IllegalArgumentException("Le nom d'utilisateur doit contenir au moins 3 caractères");
        }
        if (username.length() > 50) {
            throw new IllegalArgumentException("Le nom d'utilisateur ne peut pas dépasser 50 caractères");
        }
        // Vérifier qu'il ne contient que des caractères alphanumériques et underscores
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new IllegalArgumentException("Le nom d'utilisateur ne peut contenir que des lettres, chiffres et underscores");
        }
    }

    /**
     * Change le mot de passe de l'utilisateur.
     * Le nouveau mot de passe doit être hashé avant d'être passé à cette méthode.
     *
     * @param ancienMotDePasseHash le hash de l'ancien mot de passe (pour validation)
     * @param nouveauMotDePasseHash le hash du nouveau mot de passe
     * @throws IllegalStateException si le compte est verrouillé
     * @throws IllegalArgumentException si les mots de passe sont invalides
     */
    public void changerMotDePasse(String ancienMotDePasseHash, String nouveauMotDePasseHash) {
        if (estVerrouille()) {
            throw new IllegalStateException("Impossible de changer le mot de passe d'un compte verrouillé");
        }
        if (nouveauMotDePasseHash == null || nouveauMotDePasseHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nouveau mot de passe ne peut pas être vide");
        }
        // Note: La validation de l'ancien mot de passe se fait au niveau du service applicatif
        // car cela nécessite un PasswordEncoder qui est une dépendance infrastructure
        this.motDePasse = nouveauMotDePasseHash;
        this.initPassword = false; // L'utilisateur a changé son mot de passe
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Définit le mot de passe initial (lors de la création).
     *
     * @param motDePasseHash le hash du mot de passe (déjà hashé)
     */
    public void definirMotDePasseInitial(String motDePasseHash) {
        if (motDePasseHash == null || motDePasseHash.trim().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas être vide");
        }
        this.motDePasse = motDePasseHash;
        this.initPassword = false; // L'utilisateur devra changer son mot de passe au premier login
    }

    /**
     * Verrouille le compte utilisateur.
     */
    public void verrouiller() {
        this.locked = true;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Déverrouille le compte utilisateur.
     */
    public void deverrouiller() {
        this.locked = false;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si le compte est verrouillé.
     *
     * @return true si le compte est verrouillé, false sinon
     */
    public boolean estVerrouille() {
        return Boolean.TRUE.equals(locked);
    }

    /**
     * Vérifie si l'utilisateur doit changer son mot de passe.
     *
     * @return true si l'utilisateur doit changer son mot de passe, false sinon
     */
    public boolean doitChangerMotDePasse() {
        return Boolean.FALSE.equals(initPassword);
    }

    /**
     * Change le rôle de l'utilisateur.
     *
     * @param roleId l'ID du nouveau rôle
     */
    public void changerRole(Long roleId) {
        this.fkRole = roleId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Met à jour le nom complet de l'utilisateur.
     *
     * @param nom le nom
     * @param postnom le postnom
     * @param prenom le prénom
     */
    public void mettreAJourNomComplet(String nom, String postnom, String prenom) {
        this.nom = nom != null ? nom.trim() : null;
        this.postnom = postnom != null ? postnom.trim() : null;
        this.prenom = prenom != null ? prenom.trim() : null;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si l'utilisateur peut se connecter.
     * Un utilisateur peut se connecter si :
     * <ul>
     *   <li>Son compte n'est pas verrouillé</li>
     *   <li>Il a un mot de passe défini</li>
     * </ul>
     *
     * @return true si l'utilisateur peut se connecter, false sinon
     */
    public boolean peutSeConnecter() {
        return !estVerrouille() && motDePasse != null && !motDePasse.trim().isEmpty();
    }
}

