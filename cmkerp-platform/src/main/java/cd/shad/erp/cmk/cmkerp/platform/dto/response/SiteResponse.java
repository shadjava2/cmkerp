package cd.shad.erp.cmk.cmkerp.platform.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour un site.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SiteResponse {

  private Long id;
  private String designation;
  private String abbreviation;
  private String adresse;
  private Boolean bloquer;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreatedId;
  private Long userUpdatedId;
}

