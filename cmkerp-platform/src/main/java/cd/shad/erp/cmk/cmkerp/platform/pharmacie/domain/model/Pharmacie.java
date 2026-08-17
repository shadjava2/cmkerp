package cd.shad.erp.cmk.cmkerp.platform.pharmacie.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Agrégat Root pour le domaine Pharmacie - Pharmacie hospitalière.
 *
 * <p>Une Pharmacie est un agrégat qui représente une pharmacie au sein d'un site hospitalier.
 * Elle peut avoir différents types et est associée à un site.
 *
 * <p>Méthodes métier disponibles :
 * <ul>
 *   <li>{@link #validerCodeImmo(String)} - Valider que le code immobilier respecte les règles</li>
 *   <li>{@link #associerASite(Long)} - Associer la pharmacie à un site</li>
 *   <li>{@link #changerDesignation(String)} - Changer la désignation de la pharmacie</li>
 * </ul>
 */
@Entity
@Table(name = "pharmacies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pharmacie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fkSite")
    private Long fkSite;

    private String designation;       // longtext

    @Column(name = "typepharmacie")
    private String typePharmacie;     // enum en base, String côté Java

    private String codeimmo;

    @Column(name = "typehospi")
    private String typeHospi;         // enum en base, String côté Java

    @Column(name = "datecreate")
    private LocalDateTime dateCreate;

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreatedid")
    private Long userCreatedId;

    @Column(name = "userupdateid")
    private Long userUpdatedId;

    // ============================================
    // MÉTHODES MÉTIER - Règles de l'agrégat
    // ============================================

    /**
     * Valide que le code immobilier respecte les règles métier.
     * Invariant : le codeImmo doit être unique par site (vérifié au niveau du repository/service).
     *
     * @param codeImmo le code immobilier à valider
     * @throws IllegalArgumentException si le code est invalide
     */
    public static void validerCodeImmo(String codeImmo) {
        if (codeImmo != null && !codeImmo.trim().isEmpty()) {
            if (codeImmo.length() > 50) {
                throw new IllegalArgumentException("Le code immobilier ne peut pas dépasser 50 caractères");
            }
        }
    }

    /**
     * Associe la pharmacie à un site.
     *
     * @param siteId l'ID du site
     */
    public void associerASite(Long siteId) {
        if (siteId == null) {
            throw new IllegalArgumentException("L'ID du site ne peut pas être null");
        }
        this.fkSite = siteId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Change la désignation de la pharmacie.
     *
     * @param nouvelleDesignation la nouvelle désignation
     */
    public void changerDesignation(String nouvelleDesignation) {
        if (nouvelleDesignation == null || nouvelleDesignation.trim().isEmpty()) {
            throw new IllegalArgumentException("La désignation de la pharmacie ne peut pas être vide");
        }
        this.designation = nouvelleDesignation.trim();
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Change le code immobilier de la pharmacie avec validation.
     *
     * @param nouveauCodeImmo le nouveau code immobilier
     */
    public void changerCodeImmo(String nouveauCodeImmo) {
        validerCodeImmo(nouveauCodeImmo);
        this.codeimmo = nouveauCodeImmo != null ? nouveauCodeImmo.trim() : null;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Change le type de pharmacie.
     *
     * @param nouveauType le nouveau type
     */
    public void changerTypePharmacie(String nouveauType) {
        this.typePharmacie = nouveauType != null ? nouveauType.trim() : null;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si la pharmacie a un code immobilier.
     *
     * @return true si la pharmacie a un code immobilier, false sinon
     */
    public boolean aCodeImmo() {
        return codeimmo != null && !codeimmo.trim().isEmpty();
    }

    /**
     * Vérifie si la pharmacie est associée à un site.
     *
     * @return true si la pharmacie est associée à un site, false sinon
     */
    public boolean estAssocieeASite() {
        return fkSite != null;
    }
}

