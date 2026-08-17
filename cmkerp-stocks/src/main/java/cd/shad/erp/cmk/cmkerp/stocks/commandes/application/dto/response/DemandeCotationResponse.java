package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCotationResponse {
  private Long id;
  private String numero;
  private String objet;
  private String description;
  private Long fkPharmacieDemandeur;
  private String pharmacieNom;
  private LocalDateTime dateLimiteReponse;
  private LocalDate dateLivraisonSouhaitee;
  private String lieuLivraison;
  private String conditions;
  private String statut;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private List<LigneDemandeResponse> lignes;
  private List<InvitationResponse> invitations;
  private Integer nbOffresSoumises;
  private Integer nbInvitations;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LigneDemandeResponse {
    private Long id;
    private Long fkProduit;
    private String produitNom;
    private Long fkCategorie;
    private String categorieNom;
    private BigDecimal quantite;
    private String specifications;
    private Integer ordre;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class InvitationResponse {
    private Long id;
    private Long fkDemandeCotation;
    private Long fkFournisseur;
    private String fournisseurNom;
    private String fournisseurEmail;
    private String statut;
    private LocalDateTime expiresAt;
    private LocalDateTime openedAt;
    private LocalDateTime submittedAt;
    private String publicToken;
    private Integer relances;
  }
}
