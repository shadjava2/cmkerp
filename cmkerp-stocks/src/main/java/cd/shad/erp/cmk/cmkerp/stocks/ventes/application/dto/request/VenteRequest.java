package cd.shad.erp.cmk.cmkerp.stocks.ventes.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de requête pour la création et la mise à jour d'une vente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VenteRequest {

  private Long fkEntreprise;

  private Long fkPatient;

  @NotNull(message = "La pharmacie est obligatoire")
  private Long fkPharmacie;

  private Short taux;

  private String typepaiement;

  private String raisonsortie;

  private String demandeur;

  private String fkPatientMediline;

  private String fkFicheMedicale;
}
