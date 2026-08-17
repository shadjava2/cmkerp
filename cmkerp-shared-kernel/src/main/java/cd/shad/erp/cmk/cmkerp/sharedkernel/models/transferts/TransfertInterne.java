package cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

/**
 * Agrégat Root pour le domaine Transferts Internes.
 *
 * <p>Un TransfertInterne représente un transfert de stock entre deux pharmacies/services avec :
 * <ul>
 *   <li>Pharmacie source (fkPharmacieSource) - obligatoire</li>
 *   <li>Pharmacie destination (fkPharmacieDestination) - obligatoire</li>
 *   <li>Statut : EN_ATTENTE, TRANSFEREE, ANNULEE, RECEPTIONNEE</li>
 *   <li>Commentaire (commentaire) - optionnel</li>
 * </ul>
 *
 * <p>Règles métier :
 * <ul>
 *   <li>Statut initial : EN_ATTENTE</li>
 *   <li>Validation : passe à TRANSFEREE et déduit le stock source</li>
 *   <li>Réception : passe à RECEPTIONNEE et crédite le stock destination</li>
 *   <li>Annulation : possible seulement si EN_ATTENTE</li>
 * </ul>
 */
@Entity
@Table(name = "transfert_interne", indexes = {
    @Index(name = "index_transfert_interne_source", columnList = "fkPharmacieSource,statut,datecreate"),
    @Index(name = "index_transfert_interne_destination", columnList = "fkPharmacieDestination")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class TransfertInterne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fkPharmacieSource", nullable = false)
    private Long fkPharmacieSource;

    @Column(name = "fkPharmacieDestination", nullable = false)
    private Long fkPharmacieDestination;

    @Column(name = "statut", length = 50)
    @Builder.Default
    private StatutTransfertInterne statut = StatutTransfertInterne.EN_ATTENTE;

    @Column(name = "commentaire", columnDefinition = "MEDIUMTEXT")
    private String commentaire;

    @Column(name = "perime")
    private Boolean perime;

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
     * Enum pour le statut du transfert interne.
     * Les valeurs correspondent à l'ENUM MySQL : 'EN ATTENTE','TRANSFEREE','ANNULEE','RECEPTIONNEE'
     */
    public enum StatutTransfertInterne {
        EN_ATTENTE("EN ATTENTE"),
        TRANSFEREE("TRANSFEREE"),
        ANNULEE("ANNULEE"),
        RECEPTIONNEE("RECEPTIONNEE");

        private final String dbValue;

        StatutTransfertInterne(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static StatutTransfertInterne fromDbValue(String dbValue) {
            if (dbValue == null || dbValue.trim().isEmpty()) {
                return EN_ATTENTE;
            }
            String trimmed = dbValue.trim();

            for (StatutTransfertInterne statut : values()) {
                if (statut.dbValue.equals(trimmed)) {
                    return statut;
                }
            }

            // Fallback : essayer de matcher en remplaçant les espaces par des underscores
            String normalized = trimmed.replace(" ", "_").replace("-", "_").toUpperCase();
            try {
                return valueOf(normalized);
            } catch (IllegalArgumentException e) {
                return EN_ATTENTE; // Valeur par défaut
            }
        }
    }

    /**
     * Valide le transfert interne (passe le statut à TRANSFEREE).
     *
     * @param userId ID de l'utilisateur qui valide
     * @throws IllegalStateException si le transfert est déjà validé ou annulé
     */
    public void valider(Long userId) {
        if (this.statut == StatutTransfertInterne.TRANSFEREE) {
            throw new IllegalStateException("Le transfert interne est déjà validé");
        }
        if (this.statut == StatutTransfertInterne.ANNULEE) {
            throw new IllegalStateException("Impossible de valider un transfert interne annulé");
        }

        this.statut = StatutTransfertInterne.TRANSFEREE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Réceptionne le transfert interne (passe le statut à RECEPTIONNEE).
     *
     * @param userId ID de l'utilisateur qui réceptionne
     * @throws IllegalStateException si le transfert n'est pas TRANSFEREE ou déjà RECEPTIONNEE/ANNULEE
     */
    public void receptionner(Long userId) {
        if (this.statut == StatutTransfertInterne.RECEPTIONNEE) {
            throw new IllegalStateException("Le transfert interne est déjà réceptionné");
        }
        if (this.statut == StatutTransfertInterne.ANNULEE) {
            throw new IllegalStateException("Impossible de réceptionner un transfert interne annulé");
        }
        if (this.statut != StatutTransfertInterne.TRANSFEREE) {
            throw new IllegalStateException("Impossible de réceptionner un transfert interne qui n'est pas TRANSFEREE");
        }

        this.statut = StatutTransfertInterne.RECEPTIONNEE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Annule le transfert interne (passe le statut à ANNULEE).
     * Possible seulement si EN_ATTENTE.
     *
     * @param userId ID de l'utilisateur qui annule
     * @throws IllegalStateException si le transfert est déjà validé ou annulé
     */
    public void annuler(Long userId) {
        if (this.statut == StatutTransfertInterne.ANNULEE) {
            throw new IllegalStateException("Le transfert interne est déjà annulé");
        }
        if (this.statut == StatutTransfertInterne.TRANSFEREE) {
            throw new IllegalStateException("Impossible d'annuler un transfert interne validé");
        }
        if (this.statut == StatutTransfertInterne.RECEPTIONNEE) {
            throw new IllegalStateException("Impossible d'annuler un transfert interne déjà réceptionné");
        }

        this.statut = StatutTransfertInterne.ANNULEE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si le transfert interne peut être annulé.
     */
    public boolean peutEtreAnnule() {
        return this.statut == StatutTransfertInterne.EN_ATTENTE;
    }
}

