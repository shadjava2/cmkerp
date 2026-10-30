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
public class FacturationSyncCompareResponse {

  private boolean beachAvailable;
  private boolean chAvailable;
  private int priceDiffCount;
  private int missingOnBeachCount;
  private int missingOnChCount;
  private int equalPriceCount;

  @Builder.Default
  private List<PriceDiffItem> priceDiffs = new ArrayList<>();

  @Builder.Default
  private List<MissingProductItem> missingOnBeach = new ArrayList<>();

  @Builder.Default
  private List<MissingProductItem> missingOnCh = new ArrayList<>();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PriceDiffItem {
    private String code;
    private String designation;
    private String forme;
    private String dosage;
    private Double pauBeach;
    private Double pauCh;
    private Double ecart;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MissingProductItem {
    private String code;
    private String designation;
    private String forme;
    private String dosage;
    private Double pau;
    private Double stock;
  }
}
