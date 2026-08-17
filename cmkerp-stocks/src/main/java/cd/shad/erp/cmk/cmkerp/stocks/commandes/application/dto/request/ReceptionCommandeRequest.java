package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request;

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
public class ReceptionCommandeRequest {
  @NotNull private Long fkBonCommande;
  private LocalDate dateReception;
  private String commentaire;
  @NotEmpty private List<LigneReceptionRequest> lignes;
  private Boolean validerImmediatement;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LigneReceptionRequest {
    @NotNull private Long fkLigneBonCommande;
    @NotNull private BigDecimal quantiteRecue;
    private String lot;
    private LocalDate datePeremption;
  }
}
