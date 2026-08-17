package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneReceptionCommande {
  private Long id;
  private Long fkReception;
  private Long fkLigneBonCommande;
  private BigDecimal quantiteRecue;
  private String lot;
  private LocalDate datePeremption;
  private LocalDateTime dateCreate;
}
