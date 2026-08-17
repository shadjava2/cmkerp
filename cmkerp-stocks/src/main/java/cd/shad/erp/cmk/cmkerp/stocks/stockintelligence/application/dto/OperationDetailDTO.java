package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.util.List;

public record OperationDetailDTO(
    OperationListItemDTO header,
    List<OperationLineDTO> lignes) {}
