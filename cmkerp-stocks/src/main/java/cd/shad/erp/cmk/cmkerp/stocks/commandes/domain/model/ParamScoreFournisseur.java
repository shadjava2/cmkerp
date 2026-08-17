package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParamScoreFournisseur {
  private Long id;
  private BigDecimal poidsDelais;
  private BigDecimal poidsQualite;
  private BigDecimal poidsPrix;
  private BigDecimal poidsCompletude;
  private BigDecimal poidsReactivite;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userUpdatedId;
}
