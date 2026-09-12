package uteq.edu.ec.artisync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.security.messaging.context.AuthenticationPrincipalArgumentResolver;
import org.springframework.security.messaging.context.SecurityContextChannelInterceptor;
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

    /**
     * Habilita el broker simple en memoria para {@code /topic} y {@code /queue}
     * y fija los prefijos de destino: {@code /app} para los métodos
     * {@code @MessageMapping}, {@code /user} para mensajes dirigidos a un
     * usuario específico.
     *
     * @param config registro de configuración del broker de mensajes
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Broker en memoria para suscripciones a tópicos y colas de usuario
        config.enableSimpleBroker("/topic", "/queue");
        // Prefijo para los métodos @MessageMapping en los controladores
        config.setApplicationDestinationPrefixes("/app");
        // Prefijo para mensajes dirigidos a un usuario específico
        config.setUserDestinationPrefix("/user");
    }

    /**
     * Registra el endpoint {@code /ws} con SockJS como fallback para
     * navegadores sin soporte nativo de WebSocket, restringido a los orígenes
     * configurados en {@link #allowedOrigins}.
     *
     * @param registry registro de endpoints STOMP
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins.toArray(new String[0]))
                .withSockJS();  // Fallback para navegadores sin soporte nativo WebSocket
    }

    /**
     * Encadena {@link #webSocketAuthInterceptor} (valida el JWT del handshake y
     * lo deja en {@code accessor.setUser()}) seguido de
     * {@link SecurityContextChannelInterceptor} (publica esa autenticación en
     * {@code SecurityContextHolder} para el hilo que procesa el mensaje). El
     * orden importa: invertirlo deja sin autenticación al hilo del mensaje.
     *
     * @param registration registro de interceptores del canal de entrada del cliente
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Orden importa: webSocketAuthInterceptor valida el JWT y deja la
        // Authentication en accessor.setUser(); SecurityContextChannelInterceptor
        // la toma de ahí y la publica en SecurityContextHolder para el hilo que
        // procesa el mensaje — sin este segundo interceptor,
        // AuthenticationPrincipalArgumentResolver (ver addArgumentResolvers)
        // no encuentra ninguna Authentication y resuelve el parámetro como null.
        registration.interceptors(webSocketAuthInterceptor, new SecurityContextChannelInterceptor());
    }

    /**
     * Sin este resolver, un parámetro {@code @AuthenticationPrincipal} en un
     * método {@code @MessageMapping} (ver ChatController#enviarMensajeWs) no
     * lo resuelve ningún HandlerMethodArgumentResolver registrado por
     * defecto, y Spring lo trata como si fuera el {@code @Payload} implícito:
     * intenta deserializar el cuerpo JSON del mensaje STOMP dentro de
     * CustomUserDetails y falla con MessageConversionException en cada envío
     * real por WebSocket (solo se detecta con un cliente STOMP real — las
     * pruebas unitarias que invocan el controlador directamente en Java no lo
     * ejercitan).
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new AuthenticationPrincipalArgumentResolver());
    }
}
