package cd.shad.erp.cmk.cmkerp.stocks.stockintelligence.application.dto;

public record ApprovQualityFlagsDTO(
    boolean donneesCompletes,
    boolean prixCoherent,
    boolean quantiteCoherent,
    boolean produitReference,
    boolean risqueDetecte) {}
