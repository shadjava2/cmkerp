package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionRequest {
  @NotBlank private String scope;
  @NotBlank private String justification;
  private Long fkCategorie;
  @NotEmpty private List<LigneAttributionRequest> lignes;
  private Boolean genererBonsCommande;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LigneAttributionRequest {
    @NotNull private Long fkLigneDemande;
    @NotNull private Long fkFournisseur;
    @NotNull private BigDecimal quantiteAttribuee;
    private String motif;
  }
}
