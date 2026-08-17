package cd.shad.erp.cmk.cmkerp.platform.dto.response;

/**
 * DTO de réponse pour l'écran dashboard Pharmacie.
 *
 * <p>
 * Contient les informations agrégées d'une pharmacie avec des métriques calculées :
 * <ul>
 * <li>Informations de base de la pharmacie et de son site</li>
 * <li>Indicateur d'accès de l'utilisateur courant</li>
 * <li>Nombre d'utilisateurs ayant accès à cette pharmacie</li>
 * <li>Nombre de notifications en attente pour cette pharmacie</li>
 * </ul>
 */
public record PharmacieOverviewResponse(
    Long id,
    String designation,
    String siteNom,
    String typePharmacie,
    String typeHospi,
    boolean hasAccess,
    long nbUsersWithAccess,
    long nbNotificationsEnCours
) {
}

