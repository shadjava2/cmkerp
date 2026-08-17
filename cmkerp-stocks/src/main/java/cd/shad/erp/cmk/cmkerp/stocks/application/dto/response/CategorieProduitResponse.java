package cd.shad.erp.cmk.cmkerp.stocks.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de réponse pour une CategorieProduit.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategorieProduitResponse {
    private Long id;
    private String designation;
    private String abbreviation;
}

