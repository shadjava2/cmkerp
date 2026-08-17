package cd.shad.erp.cmk.cmkerp.stocks.inventaires.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * DTO de requête pour la création et la mise à jour d'un inventaire.
 */
@Data
public class InventaireRequest {

    @NotNull(message = "La pharmacie est obligatoire")
    private Long fkPharmacie;

    private String date_debut; // Format: ISO datetime string

    // date_fin n'est pas dans le DTO car elle est mise à jour automatiquement par le backend lors de la clôture

    private String statut; // EN COURS, TERMINE, ANNULE

    private String commentaire;

    private String typeinventaire; // PHYSIQUE, AJUSTEMENT, MENSUEL, PERIME
}

