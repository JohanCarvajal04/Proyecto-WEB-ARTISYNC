package uteq.edu.ec.artisync.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.json.ProblemDetailJacksonMixin;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.List;

/**
 * OBS-AUTO-06 (A07 OWASP): limita por IP los intentos sobre las rutas publicas
 * de autenticacion mas expuestas a abuso (fuerza bruta de login/2FA, spam de
 * cuentas, mail-bombing vía forgot-password). Generaliza el antiguo
 * LoginRateLimitFilter, que solo cubria /api/auth/login.
 *
 * La cuota POR IP vive aqui; la cuota POR CUENTA vive en
 * {@link uteq.edu.ec.artisync.service.shared.IntentosAutenticacionService},
 * invocada desde AuthServiceImpl — un filtro de pre-autenticacion no puede
 * leer el cuerpo JSON de forma segura sin bufferizarlo, y AuthServiceImpl ya
 * tiene el correo validado y el resultado real de la autenticacion en mano.
 *
 * Fail-open ante caida de Redis: un control de mitigacion no debe bloquear
 * el acceso completo al sistema (a diferencia de la blacklist de JWT en
 * JwtAuthenticationFilter, que si es fail-closed).
 */
@Slf4j
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private record Politica(String ruta, String metodo, int limite, Duration ventana, String ambito) {
    }

    private static final List<Politica> POLITICAS = List.of(
            new Politica("/api/auth/login", "POST", 10, Duration.ofSeconds(60), "login"),
            new Politica("/api/auth/2fa/verify", "POST", 10, Duration.ofSeconds(60), "2fa"),
            new Politica("/api/auth/forgot-password", "POST", 5, Duration.ofMinutes(15), "recuperacion"),
            new Politica("/api/auth/reset-password", "POST", 10, Duration.ofMinutes(15), "reset"),
            new Politica("/api/auth/registro", "POST", 5, Duration.ofMinutes(60), "registro")
    );

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuthRateLimitFilter(StringRedisTemplate redisTemplate,
                                @Autowired(required = false) ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        if (objectMapper != null) {
            this.objectMapper = objectMapper;
        } else {
            this.objectMapper = new ObjectMapper()
                    .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
                    .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        // Mismo patron que CustomAuthenticationEntryPoint: asegura el soporte de
        // serializacion de ProblemDetail (RFC 7807) sin depender de si el
        // ObjectMapper inyectado ya trae el mixin de Spring MVC registrado.
        this.objectMapper.addMixIn(ProblemDetail.class, ProblemDetailJacksonMixin.class);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Politica politica = encontrarPolitica(request);
        if (politica == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String ip = ClientIpResolver.resolver(request);
        String clave = "rl:" + politica.ambito() + ":" + ip;
        try {
            Long intentos = redisTemplate.opsForValue().increment(clave);
            if (intentos != null && intentos == 1L) {
                redisTemplate.expire(clave, politica.ventana());
            }

            if (intentos != null && intentos > politica.limite()) {
                log.warn("Rate limit por IP excedido: ip={} ambito={}", ip, politica.ambito());
                escribirRespuesta429(response, request, politica.ventana());
                return;
            }
        } catch (DataAccessException e) {
            log.warn("No se pudo contactar a Redis para el rate limit ({}); se permite la solicitud (fail-open): {}",
                    politica.ambito(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private Politica encontrarPolitica(HttpServletRequest request) {
        for (Politica politica : POLITICAS) {
            if (politica.metodo().equalsIgnoreCase(request.getMethod())
                    && politica.ruta().equals(request.getRequestURI())) {
                return politica;
            }
        }
        return null;
    }

    private void escribirRespuesta429(HttpServletResponse response, HttpServletRequest request, Duration ventana)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(ventana.getSeconds()));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS,
                "Demasiados intentos. Intenta nuevamente en unos minutos.");
        problemDetail.setType(URI.create("https://artisync.dev/errors/cuota-excedida"));
        problemDetail.setTitle(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        objectMapper.writeValue(response.getWriter(), problemDetail);
    }
}
