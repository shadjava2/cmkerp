package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneDemandeCotation {
  private Long id;
  private Long fkDemandeCotation;
  private Long fkProduit;
  private Long fkCategorie;
  private BigDecimal quantite;
  private String specifications;
  private Integer ordre;
  private LocalDateTime dateCreate;
  private Long userCreatedId;
}
