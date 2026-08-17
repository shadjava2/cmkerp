package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRequest {
  @NotNull private Long fkFournisseur;
  private Long fkBonCommande;
  private Long fkReception;
  @NotNull private BigDecimal noteDelais;
  @NotNull private BigDecimal noteQualite;
  @NotNull private BigDecimal notePrix;
  @NotNull private BigDecimal noteCompletude;
  @NotNull private BigDecimal noteReactivite;
  private String commentaire;
}
