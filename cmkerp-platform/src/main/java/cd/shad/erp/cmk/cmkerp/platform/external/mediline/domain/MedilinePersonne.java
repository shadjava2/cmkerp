package cd.shad.erp.cmk.cmkerp.platform.external.mediline.domain;

/**
 * Projection lecture d'une personne Mediline ({@code t_personne}).
 */
public record MedilinePersonne(
    String code,
    String nom,
    String postnom,
    String prenom,
    String sexe) {
}
