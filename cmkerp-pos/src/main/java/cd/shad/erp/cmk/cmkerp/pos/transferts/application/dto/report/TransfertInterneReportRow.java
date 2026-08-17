package cd.shad.erp.cmk.cmkerp.pos.transferts.application.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ligne plate pour les rapports Jasper des transferts internes POS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransfertInterneReportRow {
  private Long transfertId;
  private String pharmacieSourceNom;
  private String pharmacieDestinationNom;
  private String statut;
  private String dateCreation;
  private String commentaire;
}
