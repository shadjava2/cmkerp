package cd.shad.erp.cmk.cmkerp.stocks.usage.application.dto;

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
public class UsageEventPageResponse {
  private long totalElements;
  private int page;
  private int size;
  @Builder.Default
  private List<UsageEventEntryResponse> content = new ArrayList<>();
}
