package cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain;

/**
 * Prix / stock CLINIQUE ({@code TSTOCK.PAU}, {@code TSTOCK.STINV}) pour un CODE produit.
 */
public record CliniquePrixInfo(
    String code,
    String designation,
    String forme,
    String dosage,
    Double pau,
    Double stock) {
}
