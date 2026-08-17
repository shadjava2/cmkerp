package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionCommande {
  private Long id;
  private Long fkBonCommande;
  private String numero;
  private String statut;
  private LocalDate dateReception;
  private Long fkApprovisionnement;
  private String commentaire;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreatedId;
  private Long userUpdatedId;
}
