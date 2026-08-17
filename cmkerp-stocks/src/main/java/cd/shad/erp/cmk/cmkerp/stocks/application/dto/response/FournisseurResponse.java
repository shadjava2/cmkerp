package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour un fournisseur.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FournisseurResponse {
  private Long id;
  private String nom;
  private String adresse;
  private String telephone;
  private String email;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreatedId;
  private Long userUpdatedId;
}

