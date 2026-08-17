package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour un Dosage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DosageResponse {
    private Long id;
    private String designation;
}

