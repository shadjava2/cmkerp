package cd.shad.erp.cmk.cmkerp.platform.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Remplacement transactionnel des permissions d'un rôle.
 * Seules les différences (ajouts / retraits) sont appliquées.
 */
@Data
public class RolePermissionsUpdateRequest {

  @NotNull(message = "La liste des permissions est obligatoire")
  private List<Long> permissionIds;

  /** Version optimistic optionnelle (permissionsVersion côté client). */
  private Long expectedVersion;
}
