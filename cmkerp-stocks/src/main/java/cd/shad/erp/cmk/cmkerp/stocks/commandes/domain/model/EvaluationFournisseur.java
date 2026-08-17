package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationFournisseur {
  private Long id;
  private Long fkFournisseur;
  private Long fkBonCommande;
  private Long fkReception;
  private BigDecimal noteDelais;
  private BigDecimal noteQualite;
  private BigDecimal notePrix;
  private BigDecimal noteCompletude;
  private BigDecimal noteReactivite;
  private BigDecimal scoreGlobal;
  private String commentaire;
  private LocalDateTime dateCreate;
  private Long userCreatedId;
}
