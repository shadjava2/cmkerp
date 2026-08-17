package cd.shad.erp.cmk.cmkerp.platform.mapper;

import org.springframework.stereotype.Component;

import cd.shad.erp.cmk.cmkerp.platform.dto.response.UserPermissionsResponse;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.UserPermissions;

@Component
public class UserPermissionsMapper {

  public UserPermissionsResponse toResponse(UserPermissions userPermissions) {
    if (userPermissions == null) {
      return null;
    }

    return UserPermissionsResponse.builder()
        .userId(userPermissions.getUserId())
        .roleId(userPermissions.getRoleId())
        .username(userPermissions.getUsername())
        .locked(userPermissions.isLocked())
        .siteId(userPermissions.getSiteId())
        .permissions(userPermissions.getPermissions())
        .pharmacieIds(userPermissions.getPharmacieIds())
        .build();
  }
}

