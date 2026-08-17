package cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventaireCampagne {

  public enum Statut {
    BROUILLON, EN_COURS, CLOTURE, ANNULE
  }

  private Long id;
  private String numero;
  private String libelle;
  private LocalDate dateDebut;
  private LocalDate dateFinPrevue;
  private LocalDateTime dateCloture;
  private Statut statut;
  private String perimetreService;
  private String perimetreCategorie;
  private String responsable;
  private String notes;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
  private Long userCreateId;
  private Long userUpdateId;
}
