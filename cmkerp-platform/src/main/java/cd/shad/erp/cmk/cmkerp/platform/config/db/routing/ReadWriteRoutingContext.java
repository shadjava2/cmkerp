package cd.shad.erp.cmk.cmkerp.platform.config.db.routing;

/**
 * Contexte ThreadLocal pour déterminer si une opération doit utiliser le read replica.
 *
 * <p>
 * Cette classe permet de marquer le contexte actuel comme "read-only" pour que
 * le {@link RoutingDataSource} route automatiquement les requêtes vers le read replica.
 *
 * <p>
 * Utilisation typique :
 * <pre>{@code
 * try {
 *     ReadWriteRoutingContext.setReadOnly(true);
 *     // Les requêtes JDBC suivantes utiliseront le read replica
 *     List<User> users = userRepository.findAll();
 * } finally {
 *     ReadWriteRoutingContext.clear();
 * }
 * }</pre>
 *
 * <p>
 * Ou avec l'annotation {@code @ReadOnly} :
 * <pre>{@code
 * @ReadOnly
 * public List<User> findAllUsers() {
 *     // Cette méthode utilisera automatiquement le read replica
 *     return jdbcTemplate.query("SELECT * FROM users", ...);
 * }
 * }</pre>
 *

 */
public class ReadWriteRoutingContext {

    private static final ThreadLocal<Boolean> readOnlyContext = new ThreadLocal<>();

    /**
     * Définit si le contexte actuel doit utiliser le read replica.
     *
     * @param readOnly true pour utiliser le read replica, false pour utiliser le primary
     */
    public static void setReadOnly(boolean readOnly) {
        readOnlyContext.set(readOnly);
    }

    /**
     * Vérifie si le contexte actuel doit utiliser le read replica.
     *
     * @return true si le read replica doit être utilisé, false sinon
     */
    public static boolean isReadOnly() {
        Boolean readOnly = readOnlyContext.get();
        return readOnly != null && readOnly;
    }

    /**
     * Nettoie le contexte ThreadLocal (à appeler dans un bloc finally).
     */
    public static void clear() {
        readOnlyContext.remove();
    }
}

