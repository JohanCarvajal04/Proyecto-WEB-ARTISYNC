package uteq.edu.ec.artisync.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * OBS-08 (A07 OWASP): limita los intentos de login por IP para mitigar fuerza bruta.
 * Fail-open ante caída de Redis: un control de mitigación no debe bloquear el login
 * completo del sistema (a diferencia de la blacklist de JWT, que sí es fail-closed).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String RUTA_LOGIN = "/api/auth/login";
    private static final int LIMITE_INTENTOS = 5;
    private static final Duration VENTANA = Duration.ofSeconds(60);

    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!"POST".equalsIgnoreCase(request.getMethod()) || !RUTA_LOGIN.equals(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clave = "rl:login:" + request.getRemoteAddr();
        try {
            Long intentos = redisTemplate.opsForValue().increment(clave);
            if (intentos != null && intentos == 1L) {
                redisTemplate.expire(clave, VENTANA);
            }

            if (intentos != null && intentos > LIMITE_INTENTOS) {
                log.warn("Rate limit de login excedido para IP {}", request.getRemoteAddr());
                response.setStatus(429); // HttpServletResponse no define SC_TOO_MANY_REQUESTS
                response.setHeader("Retry-After", String.valueOf(VENTANA.getSeconds()));
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"mensaje\":\"Demasiados intentos de inicio de sesión. Intenta nuevamente en unos minutos.\"}");
                return;
            }
        } catch (DataAccessException e) {
            log.warn("No se pudo contactar a Redis para el rate limit de login; se permite la solicitud (fail-open): {}",
                    e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
