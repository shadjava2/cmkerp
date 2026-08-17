package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record TransfertQualityFlagsDTO(
    boolean donneesCompletes,
    boolean quantiteCoherent,
    boolean produitReference,
    boolean risqueDetecte) {}
