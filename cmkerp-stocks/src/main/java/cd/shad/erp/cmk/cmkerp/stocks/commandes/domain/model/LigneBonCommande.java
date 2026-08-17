package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneBonCommande {
  private Long id;
  private Long fkBonCommande;
  private Long fkLigneDemande;
  private Long fkProduit;
  private BigDecimal quantiteCommandee;
  private BigDecimal quantiteRecue;
  private BigDecimal prixUnitaireUsd;
  private BigDecimal montantLigneUsd;
  private BigDecimal prixOriginal;
  private String devise;
  private BigDecimal taux;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;

  public BigDecimal quantiteReste() {
    BigDecimal cmd = quantiteCommandee != null ? quantiteCommandee : BigDecimal.ZERO;
    BigDecimal rec = quantiteRecue != null ? quantiteRecue : BigDecimal.ZERO;
    return cmd.subtract(rec).max(BigDecimal.ZERO);
  }
}
