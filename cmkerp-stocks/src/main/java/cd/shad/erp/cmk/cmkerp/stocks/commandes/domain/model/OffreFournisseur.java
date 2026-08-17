package cd.shad.erp.cmk.cmkerp.stocks.commandes.domain.model;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffreFournisseur {
  private Long id;
  private Long fkInvitation;
  private Long fkDemandeCotation;
  private Long fkFournisseur;
  private String devise;
  private BigDecimal tauxDeclare;
  private LocalDate validiteJusquau;
  private BigDecimal fraisLivraison;
  private String conditions;
  private String statut;
  private Integer versionNo;
  private LocalDateTime dateSoumission;
  private LocalDateTime lockedAt;
  private String idempotenceSubmitKey;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
}
