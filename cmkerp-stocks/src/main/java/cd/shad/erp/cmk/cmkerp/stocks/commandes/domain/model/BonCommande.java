package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BonCommande {
  private Long id;
  private String numero;
  private Long fkDemandeCotation;
  private Long fkAttribution;
  private Long fkFournisseur;
  private Long fkPharmacie;
  private Long fkEchangeDevise;
  private String statut;
  private BigDecimal montantTotalUsd;
  private LocalDate dateCommande;
  private LocalDate dateLivraisonPrevue;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreatedId;
  private Long userUpdatedId;

  public boolean isEnRetard() {
    if (dateLivraisonPrevue == null) {
      return false;
    }
    if ("TOTALEMENT_LIVRE".equals(statut) || "CLOTURE".equals(statut) || "ANNULE".equals(statut)) {
      return false;
    }
    return dateLivraisonPrevue.isBefore(LocalDate.now());
  }
}
