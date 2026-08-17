package cd.shad.erp.cmk.cmkerp.gateway.websocket;

/**
 * ✅ SUPPRIMÉ : WebSocket complètement désactivé
 *
 * Cette classe est conservée uniquement pour éviter les erreurs de compilation. Toute la
 * configuration WebSocket a été supprimée.
 *
 * Pour réactiver WebSocket : 1. Décommenter l'annotation @EnableWebSocketMessageBroker 2.
 * Décommenter les méthodes ci-dessous 3. Ajouter la dépendance spring-boot-starter-websocket dans
 * pom.xml
 */
/*
 * import org.springframework.context.annotation.Configuration; import
 * org.springframework.messaging.simp.config.ChannelRegistration; import
 * org.springframework.messaging.simp.config.MessageBrokerRegistry; import
 * org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker; import
 * org.springframework.web.socket.config.annotation.StompEndpointRegistry; import
 * org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer; import
 * lombok.RequiredArgsConstructor;
 * 
 * @Configuration // @EnableWebSocketMessageBroker
 * 
 * @RequiredArgsConstructor public class WebSocketConfig implements WebSocketMessageBrokerConfigurer
 * {
 * 
 * private final WebSocketSecurityInterceptor webSocketSecurityInterceptor;
 * 
 * @Override public void configureMessageBroker(MessageBrokerRegistry config) {
 * config.enableSimpleBroker("/topic", "/queue"); config.setApplicationDestinationPrefixes("/app");
 * config.setUserDestinationPrefix("/user"); }
 * 
 * @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
 * registry.addEndpoint("/ws") .setAllowedOriginPatterns("*") .withSockJS(); }
 * 
 * @Override public void configureClientInboundChannel(ChannelRegistration registration) {
 * registration.interceptors(webSocketSecurityInterceptor); } }
 */
