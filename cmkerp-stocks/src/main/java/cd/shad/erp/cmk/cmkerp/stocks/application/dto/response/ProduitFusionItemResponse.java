package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Produit CMKERP pour l'écran fusion / liaison CLINIQUE. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduitFusionItemResponse {
  private Long id;
  private String codebarre;
  private String nomcommercial;
  private String nomscientifique;
  private String forme;
  private String dosage;
  private String conditionnement;
  private String categorie;
  private BigDecimal prixachat;
  private String codeClinique;
}
