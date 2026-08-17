package cd.shad.erp.cmk.cmkerp.platform.approvisionnements.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DTO de réponse pour un échange de devise (pour combo).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EchangeDeviseResponse {
    private Long id;
    private String monnaieprincipale;
    private Float tauxechange;
    private String monnaieechange;
    private String symbole;
    private LocalDateTime dateCreate;
    private LocalDateTime dateUpdate;
}



