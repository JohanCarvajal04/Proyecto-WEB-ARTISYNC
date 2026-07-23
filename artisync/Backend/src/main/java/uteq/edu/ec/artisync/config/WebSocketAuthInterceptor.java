package uteq.edu.ec.artisync.config;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.security.CustomUserDetailsService;
import uteq.edu.ec.artisync.security.JwtService;

import java.util.Date;

/**
 * Interceptor de canal STOMP que valida el JWT en el evento CONNECT.
 * Extrae el token del header "Authorization" y establece el Principal
 * autenticado en el StompHeaderAccessor para que Spring Security
 * pueda identificar al usuario en toda la sesión WebSocket.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Claims claims = jwtService.extraerTodosLosClaims(token);
                    String username = jwtService.extraerUsername(token);
                    Date expiration = claims.getExpiration();

                    if (username != null && expiration.after(new Date())) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails, null, userDetails.getAuthorities());
                        // Establece el principal para toda la sesión WebSocket
                        accessor.setUser(authToken);
                        log.debug("WebSocket autenticado para usuario: {}", username);
                    } else {
                        log.warn("WebSocket CONNECT rechazado: token expirado o usuario nulo");
                    }
                } catch (Exception e) {
                    log.warn("WebSocket CONNECT rechazado: token inválido — {}", e.getMessage());
                }
            } else {
                log.warn("WebSocket CONNECT sin header Authorization");
            }
        }

        return message;
    }
}
