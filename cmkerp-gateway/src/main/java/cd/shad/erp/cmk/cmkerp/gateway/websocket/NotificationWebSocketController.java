package cd.shad.erp.cmk.cmkerp.gateway.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Contrôleur WebSocket pour les notifications temps réel.
 *
 * <p>
 * Exemples d'utilisation :
 * <ul>
 * <li>Broadcast : Envoyer une notification à tous les utilisateurs connectés</li>
 * <li>User-specific : Envoyer une notification à un utilisateur spécifique</li>
 * </ul>
 *
 * <p>
 * Client JavaScript exemple :
 * 
 * <pre>{@code
 * const socket = new SockJS('/ws');
 * const stompClient = Stomp.over(socket);
 * stompClient.connect({}, () => {
 *   stompClient.subscribe('/topic/notifications', (message) => {
 *     console.log(JSON.parse(message.body));
 *   });
 * });
 * }</pre>
 *
 * 
 */
/**
 * ✅ SUPPRIMÉ : WebSocket complètement désactivé Ce contrôleur est désactivé pour éviter toute
 * utilisation de WebSocket.
 */
// @Controller
public class NotificationWebSocketController {

  private static final Logger log = LoggerFactory.getLogger(NotificationWebSocketController.class);

  private final SimpMessagingTemplate messagingTemplate;

  public NotificationWebSocketController(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Exemple : Broadcast d'une notification à tous les clients connectés.
   *
   * <p>
   * Le client peut envoyer un message à `/app/notification.broadcast` et tous les clients abonnés à
   * `/topic/notifications` recevront la réponse.
   */
  @MessageMapping("/notification.broadcast")
  @SendTo("/topic/notifications")
  public NotificationMessage broadcastNotification(@Payload NotificationMessage message) {
    log.debug("Broadcast notification reçu : {}", message);
    return message;
  }

  /**
   * Envoie une notification à un utilisateur spécifique.
   *
   * <p>
   * Utilise le préfixe `/user` pour cibler un utilisateur par son username.
   *
   * @param username le nom d'utilisateur
   * @param message le message de notification
   */
  public void sendNotificationToUser(String username, NotificationMessage message) {
    log.debug("Envoi notification à l'utilisateur {} : {}", username, message);
    messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
  }

  /**
   * Classe DTO pour les messages de notification WebSocket.
   */
  public static class NotificationMessage {
    private String type;
    private String title;
    private String content;
    private Long timestamp;

    public NotificationMessage() {
      this.timestamp = System.currentTimeMillis();
    }

    public NotificationMessage(String type, String title, String content) {
      this.type = type;
      this.title = title;
      this.content = content;
      this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getType() {
      return type;
    }

    public void setType(String type) {
      this.type = type;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getContent() {
      return content;
    }

    public void setContent(String content) {
      this.content = content;
    }

    public Long getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(Long timestamp) {
      this.timestamp = timestamp;
    }
  }
}

