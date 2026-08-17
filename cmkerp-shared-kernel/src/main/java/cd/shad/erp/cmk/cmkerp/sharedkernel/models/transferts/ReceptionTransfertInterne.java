package cd.shad.erp.cmk.cmkerp.sharedkernel.models.transferts;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

/**
 * Agrégat Root pour le domaine Réception de Transferts Internes.
 *
 * <p>Une ReceptionTransfertInterne représente la réception d'un transfert interne par la pharmacie destination avec :
 * <ul>
 *   <li>Référence au transfert interne (fkTransfertInterne) - obligatoire</li>
 *   <li>Statut : EN_ATTENTE, RECEPTIONNEE, ANNULEE</li>
 * </ul>
 *
 * <p>Règles métier :
 * <ul>
 *   <li>Statut initial : EN_ATTENTE</li>
 *   <li>Réception complète : passe à RECEPTIONNEE</li>
 *   <li>Annulation : possible seulement si EN_ATTENTE</li>
 * </ul>
 */
@Entity
@Table(name = "reception_transfert_interne", indexes = {
    @Index(name = "index_reception_transfert_interne_fktransfert", columnList = "fkTransfertInterne,statut,datecreate"),
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class ReceptionTransfertInterne {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fkTransfertInterne", nullable = false)
    private Long fkTransfertInterne;

    @Column(name = "statut", length = 50)
    @Builder.Default
    private StatutReceptionTransfertInterne statut = StatutReceptionTransfertInterne.EN_ATTENTE;

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
     * Enum pour le statut de la réception de transfert interne.
     * Les valeurs correspondent à l'ENUM MySQL : 'EN ATTENTE','RECEPTIONNEE','ANNULEE'
     */
    public enum StatutReceptionTransfertInterne {
        EN_ATTENTE("EN ATTENTE"),
        RECEPTIONNEE("RECEPTIONNEE"),
        ANNULEE("ANNULEE");

        private final String dbValue;

        StatutReceptionTransfertInterne(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static StatutReceptionTransfertInterne fromDbValue(String dbValue) {
            if (dbValue == null || dbValue.trim().isEmpty()) {
                return EN_ATTENTE;
            }
            String trimmed = dbValue.trim();

            for (StatutReceptionTransfertInterne statut : values()) {
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
     * Réceptionne le transfert interne (passe le statut à RECEPTIONNEE).
     *
     * @param userId ID de l'utilisateur qui réceptionne
     * @throws IllegalStateException si la réception est déjà réceptionnée ou annulée
     */
    public void receptionner(Long userId) {
        if (this.statut == StatutReceptionTransfertInterne.RECEPTIONNEE) {
            throw new IllegalStateException("La réception est déjà réceptionnée");
        }
        if (this.statut == StatutReceptionTransfertInterne.ANNULEE) {
            throw new IllegalStateException("Impossible de réceptionner une réception annulée");
        }

        this.statut = StatutReceptionTransfertInterne.RECEPTIONNEE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Annule la réception (passe le statut à ANNULEE).
     * Possible seulement si EN_ATTENTE.
     *
     * @param userId ID de l'utilisateur qui annule
     * @throws IllegalStateException si la réception est déjà réceptionnée ou annulée
     */
    public void annuler(Long userId) {
        if (this.statut == StatutReceptionTransfertInterne.ANNULEE) {
            throw new IllegalStateException("La réception est déjà annulée");
        }
        if (this.statut == StatutReceptionTransfertInterne.RECEPTIONNEE) {
            throw new IllegalStateException("Impossible d'annuler une réception déjà réceptionnée");
        }

        this.statut = StatutReceptionTransfertInterne.ANNULEE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si la réception peut être annulée.
     */
    public boolean peutEtreAnnule() {
        return this.statut == StatutReceptionTransfertInterne.EN_ATTENTE;
    }
}

