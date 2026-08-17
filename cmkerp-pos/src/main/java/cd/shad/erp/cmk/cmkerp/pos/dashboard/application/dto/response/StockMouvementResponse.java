package cd.shad.erp.cmk.cmkerp.pos.dashboard.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO pour les stocks les plus / moins mouvementés (dashboard POS).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockMouvementResponse {
  private Long id;
  private String designation;
  private Float quantite;
  private String mouvementType;
  private String date;
}
