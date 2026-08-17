package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour un Conditionnement.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConditionnementResponse {
    private Long id;
    private String designation;
}

