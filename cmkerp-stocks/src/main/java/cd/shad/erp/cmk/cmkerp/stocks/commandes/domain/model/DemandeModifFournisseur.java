package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeModifFournisseur {
  private Long id;
  private Long fkFournisseur;
  private String statut;
  private String motif;
  private String commentaireDecision;
  private LocalDateTime dateDecision;
  private Long decideurId;
  private LocalDateTime dateCreate;
}
