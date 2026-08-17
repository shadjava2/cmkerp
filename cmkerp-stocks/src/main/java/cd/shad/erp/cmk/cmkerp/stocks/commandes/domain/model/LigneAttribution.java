package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneAttribution {
  private Long id;
  private Long fkAttribution;
  private Long fkLigneDemande;
  private Long fkFournisseur;
  private BigDecimal quantiteAttribuee;
  private String motif;
  private LocalDateTime dateCreate;
}
