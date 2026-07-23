package uteq.edu.ec.artisync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * Configuración WebSocket + STOMP para mensajería en tiempo real.
 * RF-14: Mensajería interna en tiempo real. Latencia ≤ 500 ms (RNF-05).
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Value("${app.cors.allowed-origins:http://localhost:4200,http://127.0.0.1:4200}")
    private List<String> allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker en memoria para suscripciones a tópicos y colas de usuario
        config.enableSimpleBroker("/topic", "/queue");
        // Prefijo para los métodos @MessageMapping en los controladores
        config.setApplicationDestinationPrefixes("/app");
        // Prefijo para mensajes dirigidos a un usuario específico
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                .withSockJS();  // Fallback para navegadores sin soporte nativo WebSocket
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Validar el JWT en cada conexión STOMP antes de procesarla
        registration.interceptors(webSocketAuthInterceptor);
    }
}
