package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

import java.util.List;

public record WhatsAppSetupDTO(
    boolean enabled,
    boolean configured,
    boolean ready,
    boolean tokenConfigured,
    boolean phoneNumberIdConfigured,
    String verifyToken,
    String webhookPath,
    String graphApiVersion,
    List<String> yamlAllowedNumbers,
    int activeNumbersCount,
    List<String> activeNumbers,
    boolean openAiEnabled,
    String hint) {}
