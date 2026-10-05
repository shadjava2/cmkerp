package cd.shad.erp.cmk.cmkerp.platform.external.clinique.domain;

/**
 * Produit CLINIQUE ({@code dbo.TPRODUIT}) — lecture seule.
 */
public record CliniqueProduit(
    String code,
    String libelle,
    String forme,
    String dosage,
    String designation) {
}
