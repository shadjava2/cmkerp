package cd.shad.erp.cmk.cmkerp.stocks.gmao.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipementMedia {

  public enum TypeMedia {
    PHOTO, DOCUMENT, VIDEO, MANUEL, CERTIFICAT, AUTRE
  }

  private Long id;
  private Long fkEquipement;
  private TypeMedia typeMedia;
  private String nomFichier;
  private String nomOriginal;
  private String contentType;
  private long tailleOctets;
  private String storageKey;
  private String legende;
  private boolean estPrincipal;
  private LocalDateTime dateCreate;
  private Long userCreateId;
}
