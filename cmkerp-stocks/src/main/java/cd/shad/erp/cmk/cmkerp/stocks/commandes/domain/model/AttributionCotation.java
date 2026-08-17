package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionCotation {
  private Long id;
  private Long fkDemandeCotation;
  private String scope;
  private String justification;
  private Long fkCategorie;
  private LocalDateTime dateCreate;
  private Long userCreatedId;
}
