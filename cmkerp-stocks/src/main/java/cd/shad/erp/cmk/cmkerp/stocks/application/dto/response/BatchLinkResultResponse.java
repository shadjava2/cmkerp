package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

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
public class BatchLinkResultResponse {
  private int requested;
  private int successCount;
  private int failureCount;
  @Builder.Default
  private List<ProduitFusionItemResponse> linked = new ArrayList<>();
  @Builder.Default
  private List<Failure> failures = new ArrayList<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Failure {
    private Long produitId;
    private String codeClinique;
    private String error;
  }
}
