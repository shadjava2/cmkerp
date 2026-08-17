package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvitationFournisseur {
  private Long id;
  private Long fkDemandeCotation;
  private Long fkFournisseur;
  private String publicToken;
  private String accessCodeHash;
  private String sessionTokenHash;
  private LocalDateTime sessionExpiresAt;
  private Integer unlockAttempts;
  private LocalDateTime unlockLockedUntil;
  private String statut;
  private LocalDateTime expiresAt;
  private LocalDateTime openedAt;
  private LocalDateTime submittedAt;
  private Integer relances;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreatedId;
  private Long userUpdatedId;
}
