package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FusionStatsResponse {
  private long erpTotal;
  private long erpLinked;
  private long erpUnlinked;
  private long cliniqueTotal;
  private long cliniqueUsed;
  private long cliniqueAvailable;
  private boolean cliniqueAvailableFlag;
}
