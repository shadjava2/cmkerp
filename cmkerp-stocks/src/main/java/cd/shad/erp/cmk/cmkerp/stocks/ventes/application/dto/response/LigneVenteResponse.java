package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour une ligne de vente.
 * Inclut le nom du produit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneVenteResponse {
    private Long id;
    private Long fkVente;
    private Long fkStock;
    private String produitNom; // Nom du produit
    private Float stockActuel; // Stock disponible (limite de sortie)
    private Float qt;
    private BigDecimal prixventes;
    private Integer horsconvention;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
    private Long userCreatedId;
    private Long userUpdatedId;
}

