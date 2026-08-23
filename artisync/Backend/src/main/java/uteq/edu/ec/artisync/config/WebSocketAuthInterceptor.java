package uteq.edu.ec.artisync.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.entity.legal.SalaChat;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.repository.legal.SalaChatRepository;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.security.CustomUserDetailsService;
import uteq.edu.ec.artisync.security.JwtService;

import java.util.Date;

/**
 * Interceptor de canal STOMP que autentica el JWT en CONNECT y autoriza las
 * suscripciones a {@code /topic/sala.*} en SUBSCRIBE.
 *
 * <p>Fail-closed en ambos pasos:
 * <ul>
 *   <li><b>CONNECT</b> — antes, sin cabecera o con un JWT inválido, solo
 *   dejaba un WARN en el log y la conexión se aceptaba igual, sin Principal.
 *   Como {@code /ws} es público (necesario para el handshake), esa validación
 *   era la única barrera real.</li>
 *   <li><b>SUBSCRIBE</b> — el CONNECT por sí solo no bastaba: cualquier
 *   usuario ya autenticado (de cualquier cuenta, sin relación con el pedido)
 *   podía suscribirse a {@code /topic/sala.&lt;id&gt;} adivinando o
 *   enumerando ids, y leer en vivo los mensajes de un chat ajeno — el
 *   {@link org.springframework.messaging.simp.config.MessageBrokerRegistry
 *   SimpleBroker} no aplica ningún control de acceso por tópico. Aquí se
 *   verifica que quien se suscribe sea el cliente o el creador del pedido
 *   dueño de esa sala.</li>
 * </ul>
 * Cualquier fallo lanza {@link MessagingException}, que hace que Spring
 * devuelva un frame STOMP ERROR y cierre la conexión — igual que un 401/403
 * en un endpoint HTTP, en vez de dejar pasar la petición como anónima o
 * ajena.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final String PREFIJO_TOPIC_SALA = "/topic/sala.";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final SalaChatRepository salaChatRepository;

    /**
     * {@code readOnly}: la autorización de SUBSCRIBE navega
     * sala → pedido → cliente/creador, relaciones {@code LAZY}. Sin una
     * transacción abierta durante ese recorrido, la sesión de Hibernate que
     * usó {@code salaChatRepository.findById} ya se había cerrado al llegar
     * a {@code sala.getPedido()...}, y el intento de cargarlas fallaba con
     * {@code LazyInitializationException} — silenciosamente para quien
     * llamaba (Spring solo devolvía un frame STOMP ERROR genérico), incluso
     * para un usuario legítimo.
     */
    @Override
    @Transactional(readOnly = true)
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            autenticarConexion(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            autorizarSuscripcion(accessor);
        }

        return message;
    }

    private void autenticarConexion(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket CONNECT rechazado: sin header Authorization");
            throw new MessagingException("Autenticación requerida para conectar al WebSocket");
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtService.extraerTodosLosClaims(token);
            String username = jwtService.extraerUsername(token);
            Date expiration = claims.getExpiration();

            if (username == null || !expiration.after(new Date())) {
                log.warn("WebSocket CONNECT rechazado: token expirado o usuario nulo");
                throw new MessagingException("Token expirado o inválido");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
            // Establece el principal para toda la sesión WebSocket
            accessor.setUser(authToken);
            log.debug("WebSocket autenticado para usuario: {}", username);
        } catch (MessagingException e) {
            throw e;
        } catch (Exception e) {
            log.warn("WebSocket CONNECT rechazado: token inválido — {}", e.getMessage());
            throw new MessagingException("Token inválido", e);
        }
    }

    private void autorizarSuscripcion(StompHeaderAccessor accessor) {
        String destino = accessor.getDestination();
        if (destino == null || !destino.startsWith(PREFIJO_TOPIC_SALA)) {
            // Otros destinos (p.ej. /user/queue/notificaciones) ya quedan
            // acotados por Spring al Principal de la propia sesión.
            return;
        }

        Long idUsuario = idUsuarioDe(accessor);
        if (idUsuario == null) {
            log.warn("SUBSCRIBE rechazado a {}: sesión sin Principal autenticado", destino);
            throw new MessagingException("No autenticado");
        }

        Long idSala = parsearIdSala(destino);
        if (idSala == null) {
            log.warn("SUBSCRIBE rechazado: destino de sala con formato inválido ({})", destino);
            throw new MessagingException("Destino de suscripción inválido");
        }

        SalaChat sala = salaChatRepository.findById(idSala).orElse(null);
        if (sala == null) {
            log.warn("SUBSCRIBE rechazado: sala {} no existe (usuario {})", idSala, idUsuario);
            throw new MessagingException("Sala no encontrada");
        }

        Pedido pedido = sala.getPedido();
        boolean esCliente = pedido.getUsuarioCliente().getIdUsuario().equals(idUsuario);
        boolean esCreador = pedido.getServicio().getPerfil().getUsuario().getIdUsuario().equals(idUsuario);
        if (!esCliente && !esCreador) {
            log.warn("SUBSCRIBE rechazado: usuario {} no participa del pedido {} (sala {})",
                    idUsuario, pedido.getIdPedido(), idSala);
            throw new MessagingException("No tiene acceso a esta sala de chat");
        }
    }

    private Long idUsuarioDe(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof Authentication autenticacion)) {
            return null;
        }
        if (!(autenticacion.getPrincipal() instanceof CustomUserDetails principal)) {
            return null;
        }
        return principal.getIdUsuario();
    }

    private Long parsearIdSala(String destino) {
        try {
            return Long.valueOf(destino.substring(PREFIJO_TOPIC_SALA.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
