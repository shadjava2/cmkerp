package cd.shad.erp.cmk.cmkerp.platform.dto.response;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RolePermissionAuditEntry {
  String eventId;
  Instant timestamp;
  Long actorUserId;
  String actorUsername;
  String action;
  Long roleId;
  String details;
}
