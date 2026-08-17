package cd.shad.erp.cmk.cmkerp.platform.site.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Agrégat Root pour le domaine Site - Site hospitalier.
 *
 * <p>Un Site est un agrégat qui représente un site hospitalier ou une structure
 * organisationnelle dans l'ERP. Chaque site peut avoir plusieurs pharmacies associées.
 *
 * <p>Méthodes métier disponibles :
 * <ul>
 *   <li>{@link #validerDesignation(String)} - Valider que la désignation respecte les règles</li>
 *   <li>{@link #bloquer()} - Bloquer le site</li>
 *   <li>{@link #debloquer()} - Débloquer le site</li>
 *   <li>{@link #estBloque()} - Vérifier si le site est bloqué</li>
 * </ul>
 */
@Entity
@Table(name = "sites")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Site {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String designation;

    private String abbreviation;

    @Column(name = "addresse")
    private String adresse;

    @Column(name = "bloquer")
    private Boolean bloquer;

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

    /**
     * Valide que la désignation du site respecte les règles métier.
     * Invariant : la désignation doit être unique (vérifié au niveau du repository/service).
     *
     * @param designation la désignation à valider
     * @throws IllegalArgumentException si la désignation est invalide
     */
    public static void validerDesignation(String designation) {
        if (designation == null || designation.trim().isEmpty()) {
            throw new IllegalArgumentException("La désignation du site ne peut pas être vide");
        }
        if (designation.length() > 255) {
            throw new IllegalArgumentException("La désignation du site ne peut pas dépasser 255 caractères");
        }
    }

    /**
     * Met à jour la désignation du site avec validation.
     *
     * @param nouvelleDesignation la nouvelle désignation
     */
    public void changerDesignation(String nouvelleDesignation) {
        validerDesignation(nouvelleDesignation);
        this.designation = nouvelleDesignation.trim();
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Met à jour l'abréviation du site.
     *
     * @param nouvelleAbbreviation la nouvelle abréviation
     */
    public void changerAbbreviation(String nouvelleAbbreviation) {
        this.abbreviation = nouvelleAbbreviation != null ? nouvelleAbbreviation.trim() : null;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Met à jour l'adresse du site.
     *
     * @param nouvelleAdresse la nouvelle adresse
     */
    public void changerAdresse(String nouvelleAdresse) {
        this.adresse = nouvelleAdresse != null ? nouvelleAdresse.trim() : null;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Bloque le site.
     */
    public void bloquer() {
        this.bloquer = true;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Débloque le site.
     */
    public void debloquer() {
        this.bloquer = false;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si le site est bloqué.
     *
     * @return true si le site est bloqué, false sinon
     */
    public boolean estBloque() {
        return Boolean.TRUE.equals(bloquer);
    }

    /**
     * Vérifie si le site est actif (non bloqué).
     *
     * @return true si le site est actif, false sinon
     */
    public boolean estActif() {
        return !estBloque();
    }
}

