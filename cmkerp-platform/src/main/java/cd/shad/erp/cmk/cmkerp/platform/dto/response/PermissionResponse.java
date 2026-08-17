package cd.shad.erp.cmk.cmkerp.platform.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour une permission (lecture seule).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionResponse {

  private Long id;
  private String nom;
  private String description;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
}

