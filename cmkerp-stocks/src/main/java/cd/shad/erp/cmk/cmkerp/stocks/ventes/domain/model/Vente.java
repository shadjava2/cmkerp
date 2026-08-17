package cd.shad.erp.cmk.cmkerp.stocks.ventes.domain.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;

/**
 * Agrégat Root pour le domaine Ventes - Sortie pour usage.
 *
 * <p>Une Vente représente une sortie pour usage avec :
 * <ul>
 *   <li>Entreprise (fkEntreprise) - optionnel, par défaut 0</li>
 *   <li>Patient (fkPatient) - optionnel, par défaut 0</li>
 *   <li>Pharmacie (fkPharmacie) - obligatoire</li>
 *   <li>Statut : EN_ATTENTE, VALIDEE, ANNULEE</li>
 *   <li>Taux de change (taux) - par défaut 0</li>
 *   <li>Type de paiement (typepaiement) - par défaut "-"</li>
 *   <li>Raison de sortie (raisonsortie)</li>
 *   <li>Demandeur (demandeur) - nom de l'utilisateur connecté par défaut</li>
 *   <li>ID Patient Mediline (fkPatientMediline) - optionnel</li>
 *   <li>ID Fiche Médicale (fkFicheMedicale) - optionnel</li>
 * </ul>
 *
 * <p>Règles métier :
 * <ul>
 *   <li>Statut initial : EN_ATTENTE</li>
 *   <li>Validation : passe à VALIDEE</li>
 *   <li>Annulation : possible seulement dans les 24h après validation</li>
 * </ul>
 */
@Entity
@Table(name = "ventes", indexes = {
    @Index(name = "index_vente_pharmacie", columnList = "fkPharmacie,statut,datecreate"),
    @Index(name = "index_vente_patient", columnList = "fkPatient")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class Vente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "fkEntreprise")
    @Builder.Default
    private Long fkEntreprise = 0L;

    @Column(name = "fkPatient")
    @Builder.Default
    private Long fkPatient = 0L;

    @Column(name = "fkPharmacie", nullable = false)
    private Long fkPharmacie;

    @Column(name = "statut", nullable = false, length = 50)
    @Builder.Default
    private StatutVente statut = StatutVente.EN_ATTENTE;

    @Column(name = "taux")
    @Builder.Default
    private Short taux = 0;

    @Column(name = "typepaiement", length = 255)
    @Builder.Default
    private String typepaiement = "-";

    @Column(name = "raisonsortie", length = 255)
    private String raisonsortie;

    @Column(name = "demandeur", length = 255)
    private String demandeur;

    @Column(name = "fkPatientMediline", length = 255)
    private String fkPatientMediline;

    @Column(name = "fkFicheMedicale", length = 255)
    private String fkFicheMedicale;

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
     * Enum pour le statut de la vente.
     * Les valeurs correspondent à l'ENUM MySQL : 'EN ATTENTE','PAYEE','ANNULEE','FACTUREE','SORTIE-USAGE','ANNULEE-REMBOURSE','ORDONNANCE EN ATTENTE'
     * Note: VALIDEE est un alias de PAYEE pour la cohérence avec le frontend.
     */
    public enum StatutVente {
        EN_ATTENTE("EN ATTENTE"),
        PAYEE("PAYEE"),
        VALIDEE("PAYEE"), // Alias de PAYEE pour la cohérence avec le frontend
        ANNULEE("ANNULEE"),
        FACTUREE("FACTUREE"),
        SORTIE_USAGE("SORTIE-USAGE"),
        ANNULEE_REMBOURSE("ANNULEE-REMBOURSE"),
        ORDONNANCE_EN_ATTENTE("ORDONNANCE EN ATTENTE");

        private final String dbValue;

        StatutVente(String dbValue) {
            this.dbValue = dbValue;
        }

        public String getDbValue() {
            return dbValue;
        }

        public static StatutVente fromDbValue(String dbValue) {
            if (dbValue == null || dbValue.trim().isEmpty()) {
                return EN_ATTENTE;
            }
            String trimmed = dbValue.trim();

            // Chercher d'abord dans les valeurs de l'enum (ordre exact)
            for (StatutVente statut : values()) {
                if (statut.dbValue.equals(trimmed)) {
                    // PAYEE est mappé à VALIDEE pour la cohérence avec le frontend
                    // mais FACTUREE doit rester FACTUREE
                    if (statut == PAYEE) {
                        return VALIDEE;
                    }
                    return statut;
                }
            }

            // Fallback : essayer de matcher en remplaçant les espaces par des underscores
            String normalized = trimmed.replace(" ", "_").replace("-", "_").toUpperCase();
            try {
                StatutVente result = valueOf(normalized);
                // PAYEE est mappé à VALIDEE pour la cohérence avec le frontend
                // mais FACTUREE doit rester FACTUREE
                if (result == PAYEE) {
                    return VALIDEE;
                }
                return result;
            } catch (IllegalArgumentException e) {
                return EN_ATTENTE; // Valeur par défaut
            }
        }
    }

    /**
     * Valide la vente avec un statut dynamique.
     * Permet de définir le statut de validation selon le contexte :
     * - SORTIE-USAGE : pour les sorties pour usage (module Stock)
     * - PAYEE : pour les ventes payées (module POS)
     * - FACTUREE : pour les ventes facturées
     * - VALIDEE : alias de PAYEE pour compatibilité
     *
     * @param userId ID de l'utilisateur qui valide
     * @param statut Statut de validation (SORTIE-USAGE, PAYEE, FACTUREE, etc.).
     *               Si null ou vide, utilise SORTIE-USAGE par défaut.
     * @throws IllegalStateException si la vente est déjà validée ou annulée
     */
    public void valider(Long userId, String statut) {
        // Vérifier que la vente n'est pas déjà validée
        if (this.statut == StatutVente.SORTIE_USAGE ||
            this.statut == StatutVente.PAYEE ||
            this.statut == StatutVente.VALIDEE ||
            this.statut == StatutVente.FACTUREE) {
            throw new IllegalStateException("La vente est déjà validée avec le statut: " + this.statut.getDbValue());
        }
        if (this.statut == StatutVente.ANNULEE || this.statut == StatutVente.ANNULEE_REMBOURSE) {
            throw new IllegalStateException("Impossible de valider une vente annulée");
        }

        // Déterminer le statut à utiliser
        if (statut != null && !statut.trim().isEmpty()) {
            // Utiliser le statut fourni si valide
            String statutTrimmed = statut.trim();
            log.info("📥 [Vente.valider] Statut de validation reçu depuis le frontend: '{}'", statutTrimmed);

            try {
                StatutVente statutFourni = StatutVente.fromDbValue(statutTrimmed);
                log.info("🔄 [Vente.valider] Statut converti depuis DB: {} ({})", statutFourni, statutFourni.getDbValue());

                // Vérifier que c'est un statut valide pour la validation (pas EN_ATTENTE ni ANNULEE)
                if (statutFourni == StatutVente.SORTIE_USAGE ||
                    statutFourni == StatutVente.PAYEE ||
                    statutFourni == StatutVente.VALIDEE ||
                    statutFourni == StatutVente.FACTUREE) {
                    this.statut = statutFourni;
                    log.info("✅ [Vente.valider] Statut de validation appliqué avec succès: {} ({})",
                            this.statut, this.statut.getDbValue());
                } else if (statutFourni == StatutVente.EN_ATTENTE ||
                          statutFourni == StatutVente.ORDONNANCE_EN_ATTENTE) {
                    // Ne pas permettre de valider avec un statut d'attente
                    log.warn("⚠️ [Vente.valider] Statut d'attente non autorisé pour validation: {}, utilisation de SORTIE_USAGE par défaut", statutFourni);
                    this.statut = StatutVente.SORTIE_USAGE;
                } else {
                    // Par défaut, utiliser SORTIE_USAGE
                    log.warn("⚠️ [Vente.valider] Statut non valide pour validation: {} ({}), utilisation de SORTIE_USAGE par défaut",
                            statutFourni, statutFourni.getDbValue());
                    this.statut = StatutVente.SORTIE_USAGE;
                }
            } catch (Exception e) {
                // En cas d'erreur, utiliser SORTIE_USAGE par défaut
                log.error("❌ [Vente.valider] Erreur lors de la conversion du statut '{}': {}", statutTrimmed, e.getMessage());
                this.statut = StatutVente.SORTIE_USAGE;
            }
        } else {
            // Par défaut, utiliser SORTIE_USAGE (pour compatibilité avec les sorties pour usage)
            log.info("📋 [Vente.valider] Aucun statut fourni, utilisation de SORTIE_USAGE par défaut (sortie pour usage)");
            this.statut = StatutVente.SORTIE_USAGE;
        }

        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Confirme la sortie pour usage (passe le statut à SORTIE-USAGE).
     * Même structure que {@link #annuler(Long)} : contrôle d'état, audit, champs métier.
     *
     * @param userId ID utilisateur
     * @param raisonsortie raison de sortie (optionnelle, conserve l'existante si null/vide)
     * @param demandeur demandeur (optionnel, conserve l'existant si null/vide)
     */
    public void sortiePourUsage(Long userId, String raisonsortie, String demandeur) {
        if (this.statut == StatutVente.SORTIE_USAGE ||
            this.statut == StatutVente.PAYEE ||
            this.statut == StatutVente.VALIDEE ||
            this.statut == StatutVente.FACTUREE) {
            throw new IllegalStateException(
                "La vente est déjà validée avec le statut: " + this.statut.getDbValue());
        }
        if (this.statut == StatutVente.ANNULEE || this.statut == StatutVente.ANNULEE_REMBOURSE) {
            throw new IllegalStateException("Impossible de faire une sortie pour usage sur une vente annulée");
        }
        if (raisonsortie != null && !raisonsortie.trim().isEmpty()) {
            this.raisonsortie = raisonsortie.trim();
        }
        if (demandeur != null && !demandeur.trim().isEmpty()) {
            this.demandeur = demandeur.trim();
        }
        this.statut = StatutVente.SORTIE_USAGE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Annule la vente (passe le statut à ANNULEE).
     * Vérifie que l'annulation est possible (dans les 24h après validation).
     */
    public void annuler(Long userId) {
        if (this.statut == StatutVente.ANNULEE || this.statut == StatutVente.ANNULEE_REMBOURSE) {
            throw new IllegalStateException("La vente est déjà annulée");
        }
        if (this.statut == StatutVente.SORTIE_USAGE || this.statut == StatutVente.PAYEE ||
            this.statut == StatutVente.VALIDEE || this.statut == StatutVente.FACTUREE) {
            // Vérifier que l'annulation est dans les 24h après validation
            if (this.dateUpdate != null) {
                LocalDateTime limiteAnnulation = this.dateUpdate.plusHours(24);
                if (LocalDateTime.now().isAfter(limiteAnnulation)) {
                    throw new IllegalStateException("Impossible d'annuler une vente validée il y a plus de 24h");
                }
            }
        }
        this.statut = StatutVente.ANNULEE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Annule la vente avec remboursement (passe le statut à ANNULEE-REMBOURSE).
     * Vérifie que l'annulation est possible (dans les 24h après validation).
     * Utilisé depuis la liste des ventes.
     */
    public void annulerAvecRemboursement(Long userId) {
        if (this.statut == StatutVente.ANNULEE || this.statut == StatutVente.ANNULEE_REMBOURSE) {
            throw new IllegalStateException("La vente est déjà annulée");
        }
        if (this.statut == StatutVente.SORTIE_USAGE || this.statut == StatutVente.PAYEE ||
            this.statut == StatutVente.VALIDEE || this.statut == StatutVente.FACTUREE) {
            // Vérifier que l'annulation est dans les 24h après validation
            if (this.dateUpdate != null) {
                LocalDateTime limiteAnnulation = this.dateUpdate.plusHours(24);
                if (LocalDateTime.now().isAfter(limiteAnnulation)) {
                    throw new IllegalStateException("Impossible d'annuler une vente validée il y a plus de 24h");
                }
            }
        }
        this.statut = StatutVente.ANNULEE_REMBOURSE;
        this.userUpdatedId = userId;
        this.dateUpdate = LocalDateTime.now();
    }

    /**
     * Vérifie si la vente peut être annulée.
     */
    public boolean peutEtreAnnule() {
        if (this.statut == StatutVente.ANNULEE || this.statut == StatutVente.ANNULEE_REMBOURSE) {
            return false;
        }
        if ((this.statut == StatutVente.SORTIE_USAGE || this.statut == StatutVente.PAYEE ||
             this.statut == StatutVente.VALIDEE || this.statut == StatutVente.FACTUREE) && this.dateUpdate != null) {
            LocalDateTime limiteAnnulation = this.dateUpdate.plusHours(24);
            return LocalDateTime.now().isBefore(limiteAnnulation);
        }
        return true; // EN_ATTENTE peut toujours être annulé
    }
}

