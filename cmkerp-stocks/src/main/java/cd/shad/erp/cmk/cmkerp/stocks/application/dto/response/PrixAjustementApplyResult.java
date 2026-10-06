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
public class PrixAjustementApplyResult {
  private int requested;
  private int successCount;
  private int skippedCount;
  private int failureCount;
  @Builder.Default
  private List<String> failures = new ArrayList<>();
}
