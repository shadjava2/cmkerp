package cd.shad.erp.cmk.cmkerp.sharedkernel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Réponse paginée standardisée (page/size).
 *
 * <p>
 * Cette classe fournit une réponse paginée uniforme pour toutes les APIs de listes.
 * Elle remplace directement l'utilisation de {@code Page<T>} de Spring Data pour
 * éviter la dépendance directe et standardiser le format de réponse.
 *
 * <p>
 * <strong>Avantages de la pagination page/size :</strong>
 * <ul>
 *   <li>Simplicité : navigation intuitive avec numéros de page</li>
 *   <li>Compatibilité : format standard utilisé par Spring Data</li>
 *   <li>Total : permet d'afficher le nombre total d'éléments</li>
 *   <li>Navigation : permet de sauter à une page spécifique</li>
 * </ul>
 *
 * <p>
 * <strong>Utilisation :</strong>
 * <pre>{@code
 * // Première page
 * GET /api/v1/stocks/products/with-stock?page=0&size=20
 * Response: {
 *   "content": [...],
 *   "page": 0,
 *   "size": 20,
 *   "totalElements": 201,
 *   "totalPages": 11,
 *   "hasNext": true,
 *   "hasPrevious": false
 * }
 *
 * // Page suivante
 * GET /api/v1/stocks/products/with-stock?page=1&size=20
 * Response: {
 *   "content": [...],
 *   "page": 1,
 *   "size": 20,
 *   "totalElements": 201,
 *   "totalPages": 11,
 *   "hasNext": true,
 *   "hasPrevious": true
 * }
 * }</pre>
 *
 * @param <T> Type des éléments dans la liste
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageResponse<T> {

    /**
     * Liste des éléments de la page actuelle.
     * Peut être vide si aucune donnée ne correspond aux critères.
     */
    private List<T> content;

    /**
     * Numéro de la page actuelle (0-based).
     * Exemple: 0 pour la première page, 1 pour la deuxième, etc.
     */
    private int page;

    /**
     * Nombre d'éléments par page.
     * Exemple: 20, 50, 100
     */
    private int size;

    /**
     * Nombre total d'éléments correspondant aux critères de recherche/filtrage.
     * Peut être supérieur au nombre d'éléments dans {@code content} si ce n'est pas la dernière page.
     */
    private long totalElements;

    /**
     * Nombre total de pages.
     * Calculé comme: {@code Math.ceil(totalElements / size)}
     */
    private int totalPages;

    /**
     * Indique s'il y a une page suivante.
     * Équivalent à: {@code page < totalPages - 1}
     */
    private boolean hasNext;

    /**
     * Indique s'il y a une page précédente.
     * Équivalent à: {@code page > 0}
     */
    private boolean hasPrevious;

    /**
     * Crée une réponse vide (aucun élément).
     *
     * @param <T> Type des éléments
     * @return PageResponse vide avec page=0, size=0, totalElements=0
     */
    public static <T> PageResponse<T> empty() {
        return PageResponse.<T>builder()
                .content(List.of())
                .page(0)
                .size(0)
                .totalElements(0)
                .totalPages(0)
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    /**
     * Crée une PageResponse à partir d'une Page Spring Data.
     *
     * @param page Page Spring Data
     * @param <T> Type des éléments
     * @return PageResponse équivalente
     */
    public static <T> PageResponse<T> fromSpringPage(Page<T> page) {
        if (page == null) {
            return empty();
        }
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Crée une PageResponse à partir d'une liste et des informations de pagination.
     *
     * @param content Liste des éléments de la page
     * @param page Numéro de la page (0-based)
     * @param size Taille de la page
     * @param totalElements Nombre total d'éléments
     * @param <T> Type des éléments
     * @return PageResponse configurée
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = totalElements > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PageResponse.<T>builder()
                .content(content != null ? content : List.of())
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .hasNext(page < totalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }
}

