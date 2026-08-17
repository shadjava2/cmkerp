package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DemandeCotation {
  private Long id;
  private String numero;
  private String objet;
  private String description;
  private Long fkPharmacieDemandeur;
  private LocalDateTime dateLimiteReponse;
  private LocalDate dateLivraisonSouhaitee;
  private String lieuLivraison;
  private String conditions;
  private String statut;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreatedId;
  private Long userUpdatedId;
}
