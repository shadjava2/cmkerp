package cd.shad.erp.cmk.cmkerp.platform.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminIntegrityResponse {

  private long adminRoleId;
  private int catalogSize;
  private int adminPermissionCount;
  private List<String> missingPermissionCodes;
  private boolean healthy;
}
