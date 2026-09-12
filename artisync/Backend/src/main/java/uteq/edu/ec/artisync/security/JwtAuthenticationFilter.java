package uteq.edu.ec.artisync.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Componente Core de Seguridad: Filtro de interceptacion para validacion JWT.
 * 
 * Propósito: Garantizar que unicamente las peticiones con un Bearer token valido y no revocado puedan acceder a endpoints protegidos.
 * 
 * Flujo interno: Se ejecuta por cada peticion HTTP (OncePerRequestFilter). Extrae el token de la cabecera Authorization, verifica la firma con JwtService, y si es valido, inyecta un Authentication en el SecurityContextHolder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TIPO_ACCESO = "access";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

    /**
     * Autentica la petición a partir de un Bearer access token, si lo hay y es válido.
     * Deja pasar sin autenticar (nunca corta la cadena) cuando no hay cabecera
     * {@code Authorization}, el token no es {@code type=access}, está expirado o
     * es inválido — el endpoint protegido es quien decide si exige autenticación.
     *
     * @param request petición HTTP en curso
     * @param response respuesta HTTP; recibe un 503 fail-closed solo si Redis
     *                 no es alcanzable para comprobar la lista negra de tokens
     * @param filterChain resto de la cadena de filtros
     * @throws IOException si falla la escritura de la respuesta 503
     * @throws ServletException propagada de {@code filterChain.doFilter}
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = jwtService.extraerTodosLosClaims(token);

            // Lista blanca (OBS-AUTO-05): solo un token con type=access autentica
            // peticiones HTTP. Antes era lista negra (rechazaba solo type=refresh),
            // lo que aceptaba en silencio cualquier token futuro sin "type" o con un
            // tipo desconocido (p. ej. el ticket pre-auth de 2FA, que es opaco y nunca
            // llega aqui, pero un JWT mal formado con otro "type" sí colaría).
            if (!TIPO_ACCESO.equals(claims.get("type"))) {
                log.debug("Token sin claim type=access rechazado en el filtro.");
                filterChain.doFilter(request, response);
                return;
            }

            try {
                String jti = claims.getId();
                if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey("jti:" + jti))) {
                    log.debug("Token revocado rechazado en filtro (JTI: {})", jti);
                    request.setAttribute("JWT_ERROR", "Token revocado u obsoleto");
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (org.springframework.dao.DataAccessException e) {
                log.error("🚨 ALERTA CRÍTICA DE SEGURIDAD (S-05/S-10): No se pudo contactar a Redis para verificar Blacklist de tokens. Rechazando solicitud por seguridad (Fail-Closed).", e);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Offering de autenticación temporalmente no disponible (Redis Blacklist inalcanzable).");
                return;
            }

            String email = claims.get("email", String.class);
            String username = email != null ? email : claims.getSubject();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // §2.4 (OBS-AUTO-05): esAccessTokenValido comprueba, ademas de la firma
                // y el titular, que la cuenta siga habilitada y no bloqueada. Antes el
                // filtro ignoraba por completo userDetails.isEnabled(), asi que una
                // cuenta suspendida seguia autenticando hasta que expirara el token
                // (hasta 24h) si la revocacion en Redis fallaba o no llegaba a tiempo.
                if (jwtService.esAccessTokenValido(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.debug("Token rechazado por esAccessTokenValido (cuenta deshabilitada o titular no coincide): {}", username);
                    request.setAttribute("JWT_ERROR", "Credenciales inválidas o cuenta deshabilitada");
                }
            }
        } catch (ExpiredJwtException e) {
            log.debug("Token JWT expirado: {}", e.getMessage());
            request.setAttribute("JWT_ERROR", "Token expirado");
        } catch (Exception e) {
            log.debug("Token JWT inválido o malformado: {}", e.getMessage());
            request.setAttribute("JWT_ERROR", "Credenciales inválidas o token malformado");
        }

        filterChain.doFilter(request, response);
    }
}

