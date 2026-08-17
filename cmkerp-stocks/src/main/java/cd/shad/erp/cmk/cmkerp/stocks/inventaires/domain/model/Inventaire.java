package cd.shad.erp.cmk.cmkerp.stocks.inventaires.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Agrégat Root pour le domaine Inventaires.
 *
 * <p>Un Inventaire représente un inventaire de stock avec :
 * <ul>
 *   <li>Pharmacie (fkPharmacie) - obligatoire</li>
 *   <li>Date de début (date_debut) - obligatoire</li>
 *   <li>Date de fin (date_fin) - mise à jour automatique lors de la clôture</li>
 *   <li>Statut : EN COURS, TERMINE, ANNULE</li>
 *   <li>Commentaire (commentaire) - optionnel</li>
 *   <li>Type d'inventaire : PHYSIQUE, AJUSTEMENT, MENSUEL, PERIME</li>
 * </ul>
 *
 * <p>Règles métier :
 * <ul>
 *   <li>Statut initial : EN COURS</li>
 *   <li>Terminaison : passe à TERMINE et met à jour date_fin</li>
 *   <li>Annulation : passe à ANNULE</li>
 * </ul>
 */
@Entity
@Table(name = "inventaires", indexes = {
    @Index(name = "index_inventaire_pharmacie", columnList = "fkPharmacie,statut,datecreate"),
    @Index(name = "index_inventaire_statut", columnList = "statut,date_debut")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Inventaire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fkPharmacie", nullable = false)
    private Long fkPharmacie;

    @Column(name = "date_debut", nullable = false)
    @Builder.Default
    private LocalDateTime date_debut = LocalDateTime.now();

    @Column(name = "date_fin")
    private LocalDateTime date_fin; // Mise à jour automatique lors de la clôture

    @Column(name = "statut", length = 50)
    @Builder.Default
    private StatutInventaire statut = StatutInventaire.EN_COURS;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "typeinventaire", length = 50)
    @Builder.Default
    private TypeInventaire typeinventaire = TypeInventaire.PHYSIQUE;

    @Column(name = "datecreate", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime dateCreate = LocalDateTime.now();

    @Column(name = "dateupdate")
    private LocalDateTime dateUpdate;

    @Column(name = "usercreateid")
    private Long userCreatedId;

    @Column(name = "userupdateid")
    private Long userUpdatedId;

    /**
     * Enum pour le statut de l'inventaire.
     * Les valeurs correspondent à l'ENUM MySQL : 'EN COURS','TERMINE','ANNULE'
     */
    public enum StatutInventaire {
        EN_COURS("EN COURS"),
        TERMINE("TERMINE"),
        ANNULE("ANNULE");

        private final String dbValue;

        StatutInventaire(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static StatutInventaire fromDbValue(String dbValue) {
            if (dbValue == null || dbValue.trim().isEmpty()) {
                return EN_COURS;
            }
            String trimmed = dbValue.trim();

            for (StatutInventaire statut : values()) {
                if (statut.dbValue.equals(trimmed)) {
                    return statut;
                }
            }

            // Fallback : essayer de matcher en remplaçant les espaces par des underscores
            String normalized = trimmed.replace(" ", "_").toUpperCase();
            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException e) {
                return EN_COURS; // Valeur par défaut
            }
        }
    }

    /**
     * Enum pour le type d'inventaire.
     * Les valeurs correspondent à l'ENUM MySQL : 'PHYSIQUE','AJUSTEMENT','MENSUEL','PERIME'
     */
    public enum TypeInventaire {
        PHYSIQUE("PHYSIQUE"),
        AJUSTEMENT("AJUSTEMENT"),
        MENSUEL("MENSUEL"),
        PERIME("PERIME");

        private final String dbValue;

        TypeInventaire(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static TypeInventaire fromDbValue(String dbValue) {
            if (dbValue == null || dbValue.trim().isEmpty()) {
                return PHYSIQUE;
            }
            String trimmed = dbValue.trim();

            for (TypeInventaire type : values()) {
                if (type.dbValue.equals(trimmed)) {
                    return type;
                }
            }

            return PHYSIQUE; // Valeur par défaut
        }
    }

    /**
     * Termine l'inventaire (passe le statut à TERMINE et met à jour date_fin).
     */
    public void terminer(Long userId) {
        if (this.statut == StatutInventaire.TERMINE) {
            throw new IllegalStateException("L'inventaire est déjà terminé");
        }
        if (this.statut == StatutInventaire.ANNULE) {
            throw new IllegalStateException("Impossible de terminer un inventaire annulé");
        }
        this.statut = StatutInventaire.TERMINE;
        this.date_fin = LocalDateTime.now();
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Annule l'inventaire (passe le statut à ANNULE).
     */
    public void annuler(Long userId) {
        if (this.statut == StatutInventaire.ANNULE) {
            throw new IllegalStateException("L'inventaire est déjà annulé");
        }
        if (this.statut == StatutInventaire.TERMINE) {
            throw new IllegalStateException("Impossible d'annuler un inventaire terminé");
        }
        this.statut = StatutInventaire.ANNULE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }
}

