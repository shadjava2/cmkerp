package cd.shad.erp.cmk.cmkerp.stocks.approvisionnements.domain.model;

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
 *   <li>Statut : EN ATTENTE, VALIDEE, ANNULEE, ANNULEE SANS MODIFICATION</li>
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
    @Column(name = "statut", nullable = false, length = 40)
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

    /** Lien optionnel vers un bon de commande (réception Mode A depuis module Commandes). */
    @Column(name = "fk_bon_commande")
    private Long fkBonCommande;

    /** Lien optionnel vers une réception commande. */
    @Column(name = "fk_reception_commande")
    private Long fkReceptionCommande;

    /**
     * Enum pour le statut de l'approvisionnement.
     */
    public enum StatutApprovisionnement {
        EN_ATTENTE,
        VALIDEE,
        /** Annulation avec retrait stock (trigger DeleteStockAfterApprov). */
        ANNULEE,
        /**
         * Annulation administrative sans retrait stock : le stock issu du bon a déjà été consommé
         * (ou partiellement). Le trigger MySQL ne doit PAS appeler DeleteStockAfterApprov.
         */
        ANNULEE_SANS_MODIFICATION
    }

    /** True si le bon est déjà dans un statut d'annulation (avec ou sans retrait stock). */
    public boolean isAnnule() {
        return this.statut == StatutApprovisionnement.ANNULEE
                || this.statut == StatutApprovisionnement.ANNULEE_SANS_MODIFICATION;
    }

    /**
     * Valide l'approvisionnement (passe le statut à VALIDEE).
     */
    public void valider(Long userId) {
        if (this.statut == StatutApprovisionnement.VALIDEE) {
            throw new IllegalStateException("L'approvisionnement est déjà validé");
        }
        if (isAnnule()) {
            throw new IllegalStateException("Impossible de valider un approvisionnement annulé");
        }
        this.statut = StatutApprovisionnement.VALIDEE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Annule l'approvisionnement.
     *
     * @param sansModificationStock si true → ANNULEE SANS MODIFICATION (pas de retrait stock) ;
     *     sinon → ANNULEE (le trigger MySQL retire le stock).
     */
    public void annuler(Long userId, boolean ignoreDelai, boolean sansModificationStock) {
        if (isAnnule()) {
            throw new IllegalStateException("L'approvisionnement est déjà annulé");
        }
        if (!ignoreDelai && this.statut == StatutApprovisionnement.VALIDEE) {
            if (this.dateUpdate != null) {
                LocalDateTime limiteAnnulation = this.dateUpdate.plusHours(24);
                if (LocalDateTime.now().isAfter(limiteAnnulation)) {
                    throw new IllegalStateException(
                            "Impossible d'annuler un approvisionnement validé il y a plus de 24h");
                }
            }
        }
        this.statut = sansModificationStock
                ? StatutApprovisionnement.ANNULEE_SANS_MODIFICATION
                : StatutApprovisionnement.ANNULEE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /** Annulation avec retrait stock (contrôle 24h). */
    public void annuler(Long userId, boolean ignoreDelai) {
        annuler(userId, ignoreDelai, false);
    }

    /** Annulation standard (avec contrôle des 24h pour les bons validés). */
    public void annuler(Long userId) {
        annuler(userId, false, false);
    }

    /**
     * Indique si une autorisation admin est nécessaire pour annuler (validé > 24h).
     */
    public boolean necessiteAutorisationAnnulation() {
        if (this.statut != StatutApprovisionnement.VALIDEE) {
            return false;
        }
        return !peutEtreAnnule();
    }

    /**
     * Vérifie si l'approvisionnement peut être annulé.
     */
    public boolean peutEtreAnnule() {
        if (isAnnule()) {
            return false;
        }
        if (this.statut == StatutApprovisionnement.VALIDEE && this.dateUpdate != null) {
            LocalDateTime limiteAnnulation = this.dateUpdate.plusHours(24);
            return LocalDateTime.now().isBefore(limiteAnnulation);
        }
        return true; // EN_ATTENTE peut toujours être annulé
    }
}

