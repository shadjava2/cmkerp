package cd.shad.erp.cmk.cmkerp.stocks.domain.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Agrégat Root pour le domaine Approvisionnements - Bon de livraison.
 *
 * <p>Un Approvisionnement représente un bon de livraison avec :
 * <ul>
 *   <li>Fournisseur (fkFournisseur)</li>
 *   <li>Pharmacie (fkPharmacie)</li>
 *   <li>Devise d'échange (fkEchangeDevise) - optionnel</li>
 *   <li>Statut : EN ATTENTE, VALIDEE, ANNULEE</li>
 *   <li>Numéro de bon de livraison (numbonliv)</li>
 *   <li>Taux de change (taux) - pris automatiquement depuis EchangeDevise</li>
 *   <li>Date de bon de livraison (datebonliv)</li>
 * </ul>
 *
 * <p>Règles métier :
 * <ul>
 *   <li>Statut initial : EN ATTENTE</li>
 *   <li>Validation : passe à VALIDEE</li>
 *   <li>Annulation : possible seulement dans les 24h après validation</li>
 * </ul>
 */
@Entity
@Table(name = "approvsionnements", indexes = {
    @Index(name = "index_appro_unique", columnList = "fkFournisseur,fkPharmacie,fkEchangeDevise"),
    @Index(name = "index_approv_optimis", columnList = "fkPharmacie,statut,datecreate")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Approvisionnement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fkFournisseur", nullable = false)
    private Long fkFournisseur;

    @Column(name = "fkPharmacie", nullable = false)
    private Long fkPharmacie;

    @Column(name = "fkEchangeDevise")
    private Long fkEchangeDevise;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutApprovisionnement statut = StatutApprovisionnement.EN_ATTENTE;

    @Column(name = "numbonliv", length = 100)
    private String numbonliv;

    @Column(name = "taux")
    private Short taux;

    @Column(name = "datebonliv")
    private LocalDate datebonliv;

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
     * Enum pour le statut de l'approvisionnement.
     */
    public enum StatutApprovisionnement {
        EN_ATTENTE,
        VALIDEE,
        ANNULEE
    }

    /**
     * Valide l'approvisionnement (passe le statut à VALIDEE).
     */
    public void valider(Long userId) {
        if (this.statut == StatutApprovisionnement.VALIDEE) {
            throw new IllegalStateException("L'approvisionnement est déjà validé");
        }
        if (this.statut == StatutApprovisionnement.ANNULEE) {
            throw new IllegalStateException("Impossible de valider un approvisionnement annulé");
        }
        this.statut = StatutApprovisionnement.VALIDEE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Annule l'approvisionnement (passe le statut à ANNULEE).
     * Vérifie que l'annulation est possible (dans les 24h après validation).
     */
    public void annuler(Long userId) {
        if (this.statut == StatutApprovisionnement.ANNULEE) {
            throw new IllegalStateException("L'approvisionnement est déjà annulé");
        }
        if (this.statut == StatutApprovisionnement.VALIDEE) {
            // Vérifier que l'annulation est dans les 24h après validation
            if (this.dateUpdate != null) {
                LocalDateTime limiteAnnulation = this.dateUpdate.plusHours(24);
                if (LocalDateTime.now().isAfter(limiteAnnulation)) {
                    throw new IllegalStateException("Impossible d'annuler un approvisionnement validé il y a plus de 24h");
                }
            }
        }
        this.statut = StatutApprovisionnement.ANNULEE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si l'approvisionnement peut être annulé.
     */
    public boolean peutEtreAnnule() {
        if (this.statut == StatutApprovisionnement.ANNULEE) {
            return false;
        }
        if (this.statut == StatutApprovisionnement.VALIDEE && this.dateUpdate != null) {
            LocalDateTime limiteAnnulation = this.dateUpdate.plusHours(24);
            return LocalDateTime.now().isBefore(limiteAnnulation);
        }
        return true; // EN_ATTENTE peut toujours être annulé
    }
}

