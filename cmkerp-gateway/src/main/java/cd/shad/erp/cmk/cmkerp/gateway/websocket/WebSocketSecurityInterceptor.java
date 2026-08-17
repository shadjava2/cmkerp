package cd.shad.erp.cmk.cmkerp.gateway.websocket;

import java.security.Principal;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import cd.shad.erp.cmk.cmkerp.sharedkernel.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Intercepteur STOMP pour sécuriser les connexions WebSocket avec JWT.
 *
 * <p>
 * Valide le token JWT lors de l'établissement de la connexion WebSocket :
 * <ul>
 * <li>Extrait le token depuis les headers STOMP (Authorization) ou query parameters</li>
 * <li>Valide le token via JwtTokenProvider</li>
 * <li>Charge les UserDetails et établit l'Authentication</li>
 * <li>Refuse la connexion si le token est invalide</li>
 * </ul>
 *
 * <p>
 * Le token peut être fourni de deux manières :
 * <ol>
 * <li>Header STOMP : {@code Authorization: Bearer <token>}</li>
 * <li>Query parameter : {@code ws://localhost:8984/ws?token=<token>}</li>
 * </ol>
 *
 *
 */
/**
 * ✅ SUPPRIMÉ : WebSocket complètement désactivé Ce composant est désactivé pour éviter toute
 * utilisation de WebSocket.
 */
@Slf4j
// @Component
@RequiredArgsConstructor
public class WebSocketSecurityInterceptor implements ChannelInterceptor {

  private final JwtTokenProvider jwtTokenProvider;
  private final cd.shad.erp.cmk.cmkerp.gateway.security.JwtUserDetailsService jwtUserDetailsService;

  @Override
  public Message<?> preSend(Message<?> message, MessageChannel channel) {
    StompHeaderAccessor accessor =
        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

    if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
      // Extraire le token depuis les headers ou query params
      String token = extractToken(accessor);

      if (token != null && jwtTokenProvider.validateToken(token)) {
        try {
          String username = jwtTokenProvider.getUsernameFromToken(token);
          UserDetails userDetails = jwtUserDetailsService.loadUserByUsername(username);

          if (userDetails != null) {
            // Créer l'authentication et l'associer à la connexion WebSocket
            Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null,
                userDetails.getAuthorities());
            accessor.setUser((Principal) auth.getPrincipal());
            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("WebSocket connection authenticated for user: {}", username);
          } else {
            log.warn("User not found for WebSocket connection: {}", username);
            throw new SecurityException("Authentication failed: user not found");
          }
        } catch (Exception e) {
          log.error("Error authenticating WebSocket connection", e);
          throw new SecurityException("Authentication failed", e);
        }
      } else {
        log.warn("Invalid or missing JWT token for WebSocket connection");
        throw new SecurityException("Authentication failed: invalid token");
      }
    }

    return message;
  }

  /**
   * Extrait le token JWT depuis les headers STOMP ou les query parameters.
   */
  private String extractToken(StompHeaderAccessor accessor) {
    // Méthode 1 : Header Authorization
    List<String> authHeaders = accessor.getNativeHeader("Authorization");
    if (authHeaders != null && !authHeaders.isEmpty()) {
      String authHeader = authHeaders.get(0);
      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        return authHeader.substring(7);
      }
    }

    // Méthode 2 : Header personnalisé Token
    List<String> tokenHeaders = accessor.getNativeHeader("Token");
    if (tokenHeaders != null && !tokenHeaders.isEmpty()) {
      return tokenHeaders.get(0);
    }

    // Méthode 3 : Query parameter (si fourni lors de l'établissement de la connexion)
    // Note: Les query params ne sont pas directement accessibles ici, mais peuvent être
    // stockés lors de la création de l'endpoint dans WebSocketConfig
    return null;
  }
}

