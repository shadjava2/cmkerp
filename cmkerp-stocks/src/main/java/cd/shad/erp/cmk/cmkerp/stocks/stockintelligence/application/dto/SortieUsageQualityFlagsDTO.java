package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record SortieUsageQualityFlagsDTO(
    boolean donneesCompletes,
    boolean quantiteCoherent,
    boolean produitReference,
    boolean risqueDetecte) {}
