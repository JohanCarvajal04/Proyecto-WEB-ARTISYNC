package uteq.edu.ec.artisync.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
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
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final StringRedisTemplate redisTemplate;

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
            if ("refresh".equals(claims.get("type"))) {
                log.debug("Intentando autenticar con refresh token en header Authorization. Rechazado.");
                filterChain.doFilter(request, response);
                return;
            }
            try {
                String jti = claims.getId();
                if (jti != null && Boolean.TRUE.equals(redisTemplate.hasKey("jti:" + jti))) {
                    log.debug("Token revocado rechazado en filtro (JTI: {})", jti);
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (org.springframework.dao.DataAccessException e) {
                log.error("🚨 ALERTA CRÍTICA DE SEGURIDAD (S-05/S-10): No se pudo contactar a Redis para verificar Blacklist de tokens. Rechazando solicitud por seguridad (Fail-Closed).", e);
                response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Servicio de autenticación temporalmente no disponible (Redis Blacklist inalcanzable).");
                return;
            }

            String username = jwtService.extraerUsername(token);
            Date expiration = claims.getExpiration();

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (username.equals(userDetails.getUsername()) && expiration.after(new Date())) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.debug("Token JWT inválido o expirado: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
