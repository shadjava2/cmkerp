package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LigneOffreFournisseur {
  private Long id;
  private Long fkOffre;
  private Long fkLigneDemande;
  private BigDecimal prixOriginal;
  private String devise;
  private BigDecimal taux;
  private Long fkEchangeDevise;
  private BigDecimal prixUsd;
  private BigDecimal prixCdf;
  private BigDecimal quantiteDisponible;
  private Integer delaiJours;
  private String substitution;
  private String commentaire;
  private LocalDateTime dateCreate;
}
