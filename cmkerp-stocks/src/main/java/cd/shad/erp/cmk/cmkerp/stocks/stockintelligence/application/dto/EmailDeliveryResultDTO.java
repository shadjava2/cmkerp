package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.util.List;

public record EmailDeliveryResultDTO(
    int totalRecipients,
    int sent,
    int failed,
    List<String> failedAddresses
) {
  public static EmailDeliveryResultDTO empty() {
    return new EmailDeliveryResultDTO(0, 0, 0, List.of());
  }
}
