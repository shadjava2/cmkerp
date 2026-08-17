package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChampModifFournisseur {
  private Long id;
  private Long fkDemandeModif;
  private String champ;
  private String valeurActuelle;
  private String valeurProposee;
  private Boolean approuve;
}
