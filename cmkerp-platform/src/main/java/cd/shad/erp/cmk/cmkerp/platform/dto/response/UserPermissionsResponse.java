package cd.shad.erp.cmk.cmkerp.platform.dto.response;

import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour les permissions d'un utilisateur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionsResponse {

  private Long userId;
  private Long roleId;
  private String username;
  private boolean locked;
  private Long siteId;
  private Set<String> permissions;
  private Set<Long> pharmacieIds;
}

