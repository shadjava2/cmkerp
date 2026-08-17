package cd.shad.erp.cmk.cmkerp.stocks.gmao.application.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EquipementMediaResponse {
  private Long id;
  private Long fkEquipement;
  private String typeMedia;
  private String nomFichier;
  private String nomOriginal;
  private String contentType;
  private long tailleOctets;
  private String legende;
  private boolean estPrincipal;
  private boolean image;
  private String contentUrl;
  private LocalDateTime dateCreate;
}
