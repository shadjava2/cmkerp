package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WhatsAppSendRequest(
    @NotBlank @Size(max = 20) String phone,
    @Size(max = 100) String label,
    Boolean actif) {}
