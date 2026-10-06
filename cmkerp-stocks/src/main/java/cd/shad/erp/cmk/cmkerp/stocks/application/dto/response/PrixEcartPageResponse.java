package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrixEcartPageResponse {
  private long erpAboveCount;
  private long erpBelowCount;
  private long equalCount;
  private boolean cliniqueAvailable;
  @Builder.Default
  private List<PrixEcartItemResponse> erpAbove = new ArrayList<>();
  @Builder.Default
  private List<PrixEcartItemResponse> erpBelow = new ArrayList<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PrixEcartItemResponse {
    private Long produitId;
    private String nomcommercial;
    private String nomscientifique;
    private String forme;
    private String dosage;
    private String conditionnement;
    private String categorie;
    private BigDecimal prixachat;
    private String codeClinique;
    private String designationClinique;
    private Double pau;
    private Double stockErp;
    private Double stockClinique;
    private BigDecimal ecart;
  }
}
