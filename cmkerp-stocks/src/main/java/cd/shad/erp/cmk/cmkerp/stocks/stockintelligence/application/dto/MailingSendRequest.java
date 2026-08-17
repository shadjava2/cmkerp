package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MailingSendRequest(
    @NotBlank @Email String mail,
    @NotNull Boolean actif
) {}
