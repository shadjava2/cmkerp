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
public class RolePermissionsUpdateResponse {

  private Long roleId;
  private List<Long> addedPermissionIds;
  private List<Long> removedPermissionIds;
  private int totalPermissions;
  private long permissionsVersion;
}
