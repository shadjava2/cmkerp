package cd.shad.erp.cmk.cmkerp.stocks.commandes.application.dto.response;

import java.util.List;
import lombok.*;

/**
 * Réponse d'envoi : cotation + mots de passe temporaires (clair, une seule fois).
 * Jamais stockés en clair en base — uniquement renvoyés ici pour copie / e-mail.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnvoiCotationResponse {
  private DemandeCotationResponse cotation;
  private List<AccesTemporaireFournisseur> accesTemporaires;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AccesTemporaireFournisseur {
    private Long invitationId;
    private Long fkFournisseur;
    private String fournisseurNom;
    private String fournisseurEmail;
    private String publicToken;
    private String lienPortail;
    /** Mot de passe temporaire en clair — unique à ce fournisseur. */
    private String motDePasseTemporaire;
  }
}
