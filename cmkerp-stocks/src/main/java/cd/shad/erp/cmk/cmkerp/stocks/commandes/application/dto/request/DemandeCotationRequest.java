package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCotationRequest {
  @NotBlank private String objet;
  private String description;
  @NotNull private Long fkPharmacieDemandeur;
  /** Date limite (yyyy-MM-dd) — convertie en fin de journée côté service. */
  private LocalDate dateLimiteReponse;
  private LocalDate dateLivraisonSouhaitee;
  private String lieuLivraison;
  private String conditions;
  @NotEmpty private List<LigneDemandeRequest> lignes;
  @NotEmpty private List<Long> fournisseurIds;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LigneDemandeRequest {
    @NotNull private Long fkProduit;
    private Long fkCategorie;
    @NotNull private BigDecimal quantite;
    private String specifications;
    private Integer ordre;
  }
}
