package cd.shad.erp.cmk.cmkerp.platform.dto.response;

/**
 * DTO de réponse pour les statistiques du dashboard Inventory.
 *
 * <p>
 * Contient toutes les métriques principales affichées sur le tableau de bord Stock.
 */
public record InventoryStatsResponse(
    /** Nombre de produits en rupture de stock */
    Integer ruptureStock,

    /** Nombre de produits qui expireront dans 3 mois */
    Integer perimeDans3Mois,

    /** Nombre de produits qui expireront dans 1 mois */
    Integer perimeDans1Mois,

    /** Nombre de produits avec achat conforme (faible risque >= 18 mois) */
    Integer achatConforme,

    /** Nombre de produits avec achat acceptable (à surveiller, entre 12 et 17 mois) */
    Integer achatAcceptable,

    /** Nombre de produits avec achat à risque élevé (entre 6 et 11 mois) */
    Integer achatRisqueEleve,

    /** Nombre de produits avec achat non conforme (à refuser, < 6 mois) */
    Integer achatNonConforme,

    /** Nombre de produits en stock dormant */
    Integer stockDormant,

    /** Nombre de stocks les plus mouvementés */
    Integer stockPlusMouvementes,

    /** Nombre de stocks les moins mouvementés */
    Integer stockMoinsMouvementes,

    /** Nombre de fournisseurs */
    Integer fournisseurs,

    /** Nombre de demandes en attente */
    Integer demandesEnAttente,

    /** Nombre de réceptions en attente */
    Integer receptionEnAttente,

    /** Nombre de produits opérationnels suivis pour la pharmacie */
    Integer produitsSuivis
) {
}

