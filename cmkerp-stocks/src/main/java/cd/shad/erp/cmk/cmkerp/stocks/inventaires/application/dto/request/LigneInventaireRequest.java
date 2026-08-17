package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request;

import lombok.Data;

/**
 * DTO de requête pour la mise à jour d'une ligne d'inventaire.
 * Note: Les lignes sont créées automatiquement par la procédure stockée,
 * on ne peut que les mettre à jour (quantité physique, commentaire).
 */
@Data
public class LigneInventaireRequest {

    private Float quantite_physique;

    private String commentaire;
}

