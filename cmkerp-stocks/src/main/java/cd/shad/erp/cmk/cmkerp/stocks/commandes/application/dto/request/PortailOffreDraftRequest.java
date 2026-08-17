package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortailOffreDraftRequest {
  private String devise;
  private BigDecimal tauxDeclare;
  private LocalDate validiteJusquau;
  private BigDecimal fraisLivraison;
  private String conditions;
  private List<LigneOffreDraft> lignes;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LigneOffreDraft {
    private Long fkLigneDemande;
    private BigDecimal prixOriginal;
    private String devise;
    private BigDecimal quantiteDisponible;
    private Integer delaiJours;
    private String substitution;
    private String commentaire;
  }
}
