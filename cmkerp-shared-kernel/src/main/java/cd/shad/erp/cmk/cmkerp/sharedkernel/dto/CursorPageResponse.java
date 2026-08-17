package cd.shad.erp.cmk.cmkerp.sharedkernel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Réponse paginée basée sur cursor (Facebook-grade pagination).
 *
 * <p>
 * Cette classe remplace {@code Page<T>} de Spring Data pour une pagination cursor-based
 * plus performante et adaptée aux feeds/listings modernes.
 *
 * <p>
 * <strong>Avantages de la pagination cursor-based :</strong>
 * <ul>
 *   <li>Performance : pas de COUNT(*) coûteux, pas d'OFFSET sur grandes tables</li>
 *   <li>Stabilité : pas de duplication/saut d'éléments lors d'insertions pendant la pagination</li>
 *   <li>Scalabilité : fonctionne bien même avec des millions d'enregistrements</li>
 * </ul>
 *
 * <p>
 * <strong>Utilisation :</strong>
 * <pre>{@code
 * // Première page (cursor = null)
 * GET /api/v1/stocks/products/with-stock?limit=20
 * Response: {
 *   "items": [...],
 *   "nextCursor": "2025-12-05T18:33:22.146",
 *   "hasMore": true
 * }
 *
 * // Page suivante
 * GET /api/v1/stocks/products/with-stock?cursor=2025-12-05T18:33:22.146&limit=20
 * Response: {
 *   "items": [...],
 *   "nextCursor": "2025-12-05T18:30:15.789",
 *   "hasMore": false
 * }
 * }</pre>
 *
 * <p>
 * <strong>Format du cursor :</strong>
 * <ul>
 *   <li>Pour les entités avec {@code dateCreate}: ISO-8601 timestamp (ex: "2025-12-05T18:33:22.146")</li>
 *   <li>Pour les entités avec {@code id} uniquement: ID numérique (ex: "12345")</li>
 *   <li>Le cursor est toujours la valeur de la clé de tri du dernier élément de la page</li>
 * </ul>
 *
 * @param <T> Type des éléments dans la liste
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CursorPageResponse<T> {

    /**
     * Liste des éléments de la page actuelle.
     * Peut être vide si aucune donnée ne correspond aux critères.
     */
    private List<T> items;

    /**
     * Cursor pour récupérer la page suivante.
     * <ul>
     *   <li>{@code null} si {@code hasMore = false} (dernière page)</li>
     *   <li>Valeur de la clé de tri du dernier élément de cette page (ex: dateCreate ou id)</li>
     * </ul>
     * <p>
     * Format: ISO-8601 timestamp pour dateCreate, ou ID numérique pour id.
     */
    private String nextCursor;

    /**
     * Indique s'il y a plus de pages disponibles.
     * <ul>
     *   <li>{@code true} si {@code items.size() == limit} (probablement plus de données)</li>
     *   <li>{@code false} si {@code items.size() < limit} (dernière page) ou si {@code items.isEmpty()}</li>
     * </ul>
     * <p>
     * <strong>Note :</strong> Cette valeur est une indication, pas une garantie absolue.
     * Si des éléments sont supprimés entre deux requêtes, il est possible que {@code hasMore = true}
     * mais que la page suivante soit vide.
     */
    private boolean hasMore;

    /**
     * Crée une réponse vide (aucun élément).
     *
     * @param <T> Type des éléments
     * @return CursorPageResponse vide avec hasMore = false et nextCursor = null
     */
    public static <T> CursorPageResponse<T> empty() {
        return CursorPageResponse.<T>builder()
                .items(List.of())
                .nextCursor(null)
                .hasMore(false)
                .build();
    }

    /**
     * Crée une réponse avec des éléments.
     *
     * @param items Liste des éléments
     * @param nextCursor Cursor pour la page suivante (peut être null)
     * @param hasMore Indique s'il y a plus de pages
     * @param <T> Type des éléments
     * @return CursorPageResponse configurée
     */
    public static <T> CursorPageResponse<T> of(List<T> items, String nextCursor, boolean hasMore) {
        return CursorPageResponse.<T>builder()
                .items(items != null ? items : List.of())
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }
}

