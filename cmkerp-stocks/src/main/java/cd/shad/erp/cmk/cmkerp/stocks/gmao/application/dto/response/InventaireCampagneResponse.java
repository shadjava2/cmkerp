package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InventaireCampagneResponse {
  private Long id;
  private String numero;
  private String libelle;
  private LocalDate dateDebut;
  private LocalDate dateFinPrevue;
  private LocalDateTime dateCloture;
  private String statut;
  private String perimetreService;
  private String perimetreCategorie;
  private String responsable;
  private String notes;
  private long totalLignes;
  private long aVerifier;
  private long presentes;
  private long absentes;
  private long deplacees;
  private long ecarts;
  private LocalDateTime dateCreate;
  private LocalDateTime dateUpdate;
}
