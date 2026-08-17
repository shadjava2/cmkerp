package cd.shad.erp.cmk.cmkerp.gateway.websocket;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import cd.shad.erp.cmk.cmkerp.sharedkernel.events.ProductUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service WebSocket pour notifier les mises à jour de produits en temps réel.
 *
 * <p>
 * Facebook-Grade : Notifie tous les clients connectés qui ont le formulaire produit ouvert
 * lorsqu'un produit est créé, modifié ou supprimé.
 *
 * <p>
 * Topics utilisés :
 * <ul>
 * <li>{@code /topic/products/updated} : Notification de mise à jour de produit</li>
 * <li>{@code /topic/products/created} : Notification de création de produit</li>
 * <li>{@code /topic/products/deleted} : Notification de suppression de produit</li>
 * <li>{@code /topic/dashboard/updated} : Notification de mise à jour du dashboard</li>
 * </ul>
 */
/**
 * ✅ SUPPRIMÉ : WebSocket complètement désactivé Ce service est désactivé pour éviter toute
 * utilisation de WebSocket.
 */
@Slf4j
// @Service
@RequiredArgsConstructor
public class ProductWebSocketService {

  private final SimpMessagingTemplate messagingTemplate;

  /**
   * Écoute les événements ProductUpdatedEvent et envoie les notifications WebSocket.
   *
   * <p>
   * Facebook-Grade : Découplage via Spring Events - le module stocks publie un événement, et ce
   * service écoute et notifie via WebSocket.
   *
   * <p>
   * Utilise @Async pour traiter l'événement de manière asynchrone et ne pas bloquer la transaction
   * principale.
   */
  @Async
  @EventListener
  public void handleProductUpdatedEvent(ProductUpdatedEvent event) {
    try {
      switch (event.getEventType()) {
        case "CREATED":
          notifyProductCreated(event.getProductId(), event.getProductData());
          // Notifier aussi le dashboard
          notifyDashboardUpdate("PRODUCT_CREATED");
          break;
        case "UPDATED":
          notifyProductUpdated(event.getProductId(), event.getProductData());
          // Notifier aussi le dashboard
          notifyDashboardUpdate("PRODUCT_UPDATED");
          break;
        case "DELETED":
          notifyProductDeleted(event.getProductId());
          // Notifier aussi le dashboard
          notifyDashboardUpdate("PRODUCT_DELETED");
          break;
        default:
          log.warn("Type d'événement inconnu: {}", event.getEventType());
      }
    } catch (Exception e) {
      log.error("Erreur lors du traitement de l'événement ProductUpdatedEvent", e);
    }
  }

  /**
   * Notifie tous les clients qu'un produit a été créé.
   *
   * @param productId l'ID du produit créé
   * @param productData les données du produit (optionnel, pour précharger le formulaire)
   */
  public void notifyProductCreated(Long productId, Object productData) {
    try {
      ProductUpdateMessage message = new ProductUpdateMessage("PRODUCT_CREATED", productId,
          productData, System.currentTimeMillis());

      messagingTemplate.convertAndSend("/topic/products/created", message);

      if (log.isDebugEnabled()) {
        log.debug("Notification WebSocket envoyée: produit créé (ID={})", productId);
      }
    } catch (Exception e) {
      log.error("Erreur lors de l'envoi de la notification WebSocket pour produit créé (ID={})",
          productId, e);
    }
  }

  /**
   * Notifie tous les clients qu'un produit a été mis à jour.
   *
   * @param productId l'ID du produit mis à jour
   * @param productData les données du produit mises à jour (optionnel)
   */
  public void notifyProductUpdated(Long productId, Object productData) {
    try {
      ProductUpdateMessage message = new ProductUpdateMessage("PRODUCT_UPDATED", productId,
          productData, System.currentTimeMillis());

      messagingTemplate.convertAndSend("/topic/products/updated", message);

      if (log.isDebugEnabled()) {
        log.debug("Notification WebSocket envoyée: produit mis à jour (ID={})", productId);
      }
    } catch (Exception e) {
      log.error(
          "Erreur lors de l'envoi de la notification WebSocket pour produit mis à jour (ID={})",
          productId, e);
    }
  }

  /**
   * Notifie tous les clients qu'un produit a été supprimé.
   *
   * @param productId l'ID du produit supprimé
   */
  public void notifyProductDeleted(Long productId) {
    try {
      ProductUpdateMessage message =
          new ProductUpdateMessage("PRODUCT_DELETED", productId, null, System.currentTimeMillis());

      messagingTemplate.convertAndSend("/topic/products/deleted", message);

      if (log.isDebugEnabled()) {
        log.debug("Notification WebSocket envoyée: produit supprimé (ID={})", productId);
      }
    } catch (Exception e) {
      log.error("Erreur lors de l'envoi de la notification WebSocket pour produit supprimé (ID={})",
          productId, e);
    }
  }

  /**
   * Notifie tous les clients que le dashboard doit être actualisé.
   *
   * @param reason la raison de l'actualisation (ex: "PRODUCT_UPDATED", "STOCK_CHANGED")
   */
  public void notifyDashboardUpdate(String reason) {
    try {
      DashboardUpdateMessage message =
          new DashboardUpdateMessage(reason, System.currentTimeMillis());

      messagingTemplate.convertAndSend("/topic/dashboard/updated", message);

      if (log.isDebugEnabled()) {
        log.debug("Notification WebSocket envoyée: dashboard à actualiser (reason={})", reason);
      }
    } catch (Exception e) {
      log.error("Erreur lors de l'envoi de la notification WebSocket pour dashboard (reason={})",
          reason, e);
    }
  }

  /**
   * Message de notification pour les mises à jour de produits.
   */
  public static class ProductUpdateMessage {
    private String type;
    private Long productId;
    private Object productData;
    private Long timestamp;

    public ProductUpdateMessage() {
      this.timestamp = System.currentTimeMillis();
    }

    public ProductUpdateMessage(String type, Long productId, Object productData, Long timestamp) {
      this.type = type;
      this.productId = productId;
      this.productData = productData;
      this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public Long getProductId() {
      return productId;
    }

    public void setProductId(Long productId) {
      this.productId = productId;
    }

    public Object getProductData() {
      return productData;
    }

    public void setProductData(Object productData) {
      this.productData = productData;
    }

    public Long getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
    }
  }

  /**
   * Message de notification pour les mises à jour du dashboard.
   */
  public static class DashboardUpdateMessage {
    private String reason;
    private Long timestamp;

    public DashboardUpdateMessage() {
      this.timestamp = System.currentTimeMillis();
    }

    public DashboardUpdateMessage(String reason, Long timestamp) {
      this.reason = reason;
      this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getReason() {
      return reason;
    }

    public void setReason(String reason) {
      this.reason = reason;
    }

    public Long getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
    }
  }
}

