package cd.shad.erp.cmk.cmkerp.sharedkernel.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Événement Spring publié lorsqu'un produit est créé, mis à jour ou supprimé.
 *
 * <p>
 * Utilisé pour découpler les modules et permettre aux services WebSocket
 * d'écouter les changements sans dépendre directement des services métier.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdatedEvent {

    /**
     * Type d'événement : CREATED, UPDATED, DELETED
     */
    private String eventType;

    /**
     * ID du produit concerné
     */
    private Long productId;

    /**
     * Données du produit (optionnel, pour précharger le formulaire)
     */
    private Object productData;

    /**
     * ID de l'utilisateur qui a effectué l'action
     */
    private Long userId;
}

