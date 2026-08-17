package cd.shad.erp.cmk.cmkerp.platform.security.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Agrégat Root pour le domaine Security - Rôle utilisateur.
 *
 * <p>Un Role est un agrégat qui encapsule :
 * <ul>
 *   <li>Les informations du rôle (nom, description)</li>
 *   <li>Les permissions associées via RolePermission</li>
 *   <li>Les invariants métier (nom unique, pas de doublons de permissions)</li>
 * </ul>
 *
 * <p>Méthodes métier disponibles :
 * <ul>
 *   <li>{@link #ajouterPermission(Long)} - Ajouter une permission au rôle</li>
 *   <li>{@link #retirerPermission(Long)} - Retirer une permission du rôle</li>
 *   <li>{@link #aPermission(Long)} - Vérifier si le rôle a une permission</li>
 *   <li>{@link #validerNom(String)} - Valider que le nom respecte les règles métier</li>
 * </ul>
 */
@Entity
@Table(name = "roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nom;

    private String description;

    @Column(name = "datecreate")
    private LocalDateTime dateCreate;

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    @Column(name = "userupdateid")
    private Long userUpdatedId;

    /**
     * Collection en mémoire des permissions associées (non persistée directement).
     * Cette collection est utilisée pour gérer les invariants de l'agrégat.
     * En production, elle sera chargée via le repository.
     */
    @Transient
    @Builder.Default
    private Set<Long> permissionIds = new HashSet<>();

    // ============================================
    // MÉTHODES MÉTIER - Règles de l'agrégat
    // ============================================

    /**
     * Valide que le nom du rôle respecte les règles métier.
     *
     * @param nom le nom à valider
     * @throws IllegalArgumentException si le nom est invalide
     */
    public static void validerNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du rôle ne peut pas être vide");
        }
        if (nom.length() > 100) {
            throw new IllegalArgumentException("Le nom du rôle ne peut pas dépasser 100 caractères");
        }
    }

    /**
     * Ajoute une permission au rôle.
     * Vérifie que la permission n'est pas déjà associée (invariant : pas de doublons).
     *
     * @param permissionId l'ID de la permission à ajouter
     * @return true si la permission a été ajoutée, false si elle était déjà présente
     */
    public boolean ajouterPermission(Long permissionId) {
        if (permissionId == null) {
            throw new IllegalArgumentException("L'ID de la permission ne peut pas être null");
        }
        if (permissionIds == null) {
            permissionIds = new HashSet<>();
        }
        return permissionIds.add(permissionId);
    }

    /**
     * Retire une permission du rôle.
     *
     * @param permissionId l'ID de la permission à retirer
     * @return true si la permission a été retirée, false si elle n'était pas présente
     */
    public boolean retirerPermission(Long permissionId) {
        if (permissionId == null) {
            throw new IllegalArgumentException("L'ID de la permission ne peut pas être null");
        }
        if (permissionIds == null) {
            return false;
        }
        return permissionIds.remove(permissionId);
    }

    /**
     * Vérifie si le rôle possède une permission spécifique.
     *
     * @param permissionId l'ID de la permission à vérifier
     * @return true si le rôle a la permission, false sinon
     */
    public boolean aPermission(Long permissionId) {
        if (permissionId == null || permissionIds == null) {
            return false;
        }
        return permissionIds.contains(permissionId);
    }

    /**
     * Vérifie si le rôle a au moins une permission.
     *
     * @return true si le rôle a des permissions, false sinon
     */
    public boolean aDesPermissions() {
        return permissionIds != null && !permissionIds.isEmpty();
    }

    /**
     * Met à jour le nom du rôle avec validation.
     *
     * @param nouveauNom le nouveau nom
     */
    public void changerNom(String nouveauNom) {
        validerNom(nouveauNom);
        this.nom = nouveauNom.trim();
    }

    /**
     * Met à jour la description du rôle.
     *
     * @param nouvelleDescription la nouvelle description
     */
    public void changerDescription(String nouvelleDescription) {
        this.description = nouvelleDescription != null ? nouvelleDescription.trim() : null;
    }
}

