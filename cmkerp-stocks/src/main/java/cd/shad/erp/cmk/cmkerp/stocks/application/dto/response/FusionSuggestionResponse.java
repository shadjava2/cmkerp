package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FusionSuggestionResponse {
  private Long produitId;
  private String nomcommercial;
  private String nomscientifique;
  private String forme;
  private String dosage;
  private String conditionnement;
  private String codeClinique;
  private String designationClinique;
  private String libelleClinique;
  private String formeClinique;
  private String dosageClinique;
  private Double pau;
  /** Score 0..1 (1 = correspondance exacte normalisée). */
  private double score;
  private String matchReason;
}
