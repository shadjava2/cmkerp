package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse simple pour un produit (liste, sans JOINs).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProduitResponse {
  private Long id;
  private String codebarre;
  private String nomcommercial;
  private String nomscientifique;
  private Long fkForme;
  private Long fkDosage;
  private Long fkConditionnement;
  private Long fkCategorie;
  private BigDecimal prixachat;
  private BigDecimal prixachatcomptable;
  private Float qtealert;
  private Float qtcritique;
  private Boolean perimable;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreatedId;
  private Long userUpdatedId;
}

