package cd.shad.erp.cmk.cmkerp.stocks.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Agrégat Root pour le domaine Stocks - Produit pharmaceutique.
 *
 * <p>Un Produit est un agrégat qui représente un produit pharmaceutique avec ses caractéristiques
 * (code-barres, nom commercial, nom scientifique, forme, dosage, conditionnement, catégorie, prix, etc.).
 *
 * <p>Relations :
 * <ul>
 *   <li>fkForme → Forme (nullable)</li>
 *   <li>fkDosage → Dosage (nullable)</li>
 *   <li>fkConditionnement → Conditionnement (nullable)</li>
 *   <li>fkCategorie → CategorieProduit (nullable)</li>
 * </ul>
 *
 * <p>Méthodes métier disponibles :
 * <ul>
 *   <li>{@link #validerCodebarre(String)} - Valider que le code-barres respecte les règles</li>
 *   <li>{@link #changerNomCommercial(String)} - Changer le nom commercial</li>
 *   <li>{@link #mettreAJourPrixAchat(BigDecimal)} - Mettre à jour le prix d'achat</li>
 *   <li>{@link #estPerimable()} - Vérifier si le produit est périssable</li>
 * </ul>
 */
@Entity
@Table(name = "produits", indexes = {
    @Index(name = "indexproduitid", columnList = "fkForme,fkDosage,fkConditionnement,fkCategorie"),
    @Index(name = "uniquecodebarre", columnList = "codebarre", unique = true),
    @Index(name = "idx_produits_fk", columnList = "fkForme,fkDosage,fkConditionnement,fkCategorie"),
    @Index(name = "idx_nomcommercial", columnList = "nomcommercial")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Produit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codebarre", length = 255, unique = true)
    private String codebarre;

    @Column(name = "nomcommercial", length = 255)
    private String nomcommercial;

    @Column(name = "nomscientifique", length = 255)
    private String nomscientifique;

    @Column(name = "fkForme")
    private Long fkForme;

    @Column(name = "fkDosage")
    private Long fkDosage;

    @Column(name = "fkConditionnement")
    private Long fkConditionnement;

    @Column(name = "fkCategorie")
    private Long fkCategorie;

    @Column(name = "prixachat", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixachat;

    @Column(name = "prixachatcomptable", precision = 10, scale = 4)
    private BigDecimal prixachatcomptable;

    @Column(name = "qtealert", nullable = false)
    @Builder.Default
    private Float qtealert = 0.0f;

    @Column(name = "qtcritique", nullable = false)
    @Builder.Default
    private Float qtcritique = 0.0f;

    @Column(name = "perimable", nullable = false)
    @Builder.Default
    private Boolean perimable = false;

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
     * Valide que le code-barres respecte les règles métier.
     * Invariant : le codebarre doit être unique (vérifié au niveau du repository/service).
     *
     * @param codebarre le code-barres à valider
     * @throws IllegalArgumentException si le code-barres est invalide
     */
    public static void validerCodebarre(String codebarre) {
        if (codebarre != null && !codebarre.trim().isEmpty()) {
            if (codebarre.length() > 255) {
                throw new IllegalArgumentException("Le code-barres ne peut pas dépasser 255 caractères");
            }
        }
    }

    /**
     * Change le code-barres du produit avec validation.
     *
     * @param nouveauCodebarre le nouveau code-barres
     */
    public void changerCodebarre(String nouveauCodebarre) {
        validerCodebarre(nouveauCodebarre);
        this.codebarre = nouveauCodebarre != null ? nouveauCodebarre.trim() : null;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Change le nom commercial du produit.
     *
     * @param nouveauNomCommercial le nouveau nom commercial
     */
    public void changerNomCommercial(String nouveauNomCommercial) {
        if (nouveauNomCommercial != null && nouveauNomCommercial.length() > 255) {
            throw new IllegalArgumentException("Le nom commercial ne peut pas dépasser 255 caractères");
        }
        this.nomcommercial = nouveauNomCommercial != null ? nouveauNomCommercial.trim() : null;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Change le nom scientifique du produit.
     *
     * @param nouveauNomScientifique le nouveau nom scientifique
     */
    public void changerNomScientifique(String nouveauNomScientifique) {
        if (nouveauNomScientifique != null && nouveauNomScientifique.length() > 255) {
            throw new IllegalArgumentException("Le nom scientifique ne peut pas dépasser 255 caractères");
        }
        this.nomscientifique = nouveauNomScientifique != null ? nouveauNomScientifique.trim() : null;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Met à jour le prix d'achat du produit.
     *
     * @param nouveauPrixAchat le nouveau prix d'achat
     * @throws IllegalArgumentException si le prix est négatif
     */
    public void mettreAJourPrixAchat(BigDecimal nouveauPrixAchat) {
        if (nouveauPrixAchat == null) {
            throw new IllegalArgumentException("Le prix d'achat ne peut pas être null");
        }
        if (nouveauPrixAchat.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix d'achat ne peut pas être négatif");
        }
        this.prixachat = nouveauPrixAchat;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Met à jour le prix d'achat comptable du produit.
     *
     * @param nouveauPrixAchatComptable le nouveau prix d'achat comptable
     */
    public void mettreAJourPrixAchatComptable(BigDecimal nouveauPrixAchatComptable) {
        if (nouveauPrixAchatComptable != null && nouveauPrixAchatComptable.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le prix d'achat comptable ne peut pas être négatif");
        }
        this.prixachatcomptable = nouveauPrixAchatComptable;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Met à jour les quantités d'alerte et critique.
     *
     * @param nouvelleQteAlert la nouvelle quantité d'alerte
     * @param nouvelleQteCritique la nouvelle quantité critique
     * @throws IllegalArgumentException si les quantités sont négatives
     */
    public void mettreAJourQuantitesAlerte(Float nouvelleQteAlert, Float nouvelleQteCritique) {
        if (nouvelleQteAlert != null && nouvelleQteAlert < 0) {
            throw new IllegalArgumentException("La quantité d'alerte ne peut pas être négative");
        }
        if (nouvelleQteCritique != null && nouvelleQteCritique < 0) {
            throw new IllegalArgumentException("La quantité critique ne peut pas être négative");
        }
        this.qtealert = nouvelleQteAlert != null ? nouvelleQteAlert : 0.0f;
        this.qtcritique = nouvelleQteCritique != null ? nouvelleQteCritique : 0.0f;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Associe le produit à une forme.
     *
     * @param formeId l'ID de la forme
     */
    public void associerForme(Long formeId) {
        this.fkForme = formeId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Associe le produit à un dosage.
     *
     * @param dosageId l'ID du dosage
     */
    public void associerDosage(Long dosageId) {
        this.fkDosage = dosageId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Associe le produit à un conditionnement.
     *
     * @param conditionnementId l'ID du conditionnement
     */
    public void associerConditionnement(Long conditionnementId) {
        this.fkConditionnement = conditionnementId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Associe le produit à une catégorie.
     *
     * @param categorieId l'ID de la catégorie
     */
    public void associerCategorie(Long categorieId) {
        this.fkCategorie = categorieId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si le produit est périssable.
     *
     * @return true si le produit est périssable, false sinon
     */
    public boolean estPerimable() {
        return Boolean.TRUE.equals(perimable);
    }

    /**
     * Marque le produit comme périssable ou non.
     *
     * @param estPerimable true si le produit est périssable, false sinon
     */
    public void definirPerimable(boolean estPerimable) {
        this.perimable = estPerimable;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si le produit a un code-barres.
     *
     * @return true si le produit a un code-barres, false sinon
     */
    public boolean aCodebarre() {
        return codebarre != null && !codebarre.trim().isEmpty();
    }
}

