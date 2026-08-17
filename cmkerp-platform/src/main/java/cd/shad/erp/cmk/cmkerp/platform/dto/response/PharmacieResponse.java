package cd.shad.erp.cmk.cmkerp.platform.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour une pharmacie.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PharmacieResponse {

  private Long id;
  private Long fkSite;
  private String siteDesignation; // Champ dérivé calculé côté service
  private String designation;
  private String typePharmacie;
  private String codeimmo;
  private String typeHospi;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreatedId;
  private Long userUpdatedId;
}

