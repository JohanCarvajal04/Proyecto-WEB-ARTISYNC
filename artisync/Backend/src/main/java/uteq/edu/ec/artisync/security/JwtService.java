package uteq.edu.ec.artisync.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secret;

    @Value("${security.jwt.expiration-time:86400000}")
    private long expirationMs;

    @Value("${security.jwt.refresh-expiration-time:604800000}")
    private long refreshExpirationMs;

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private SecretKey clave() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generarToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        List<String> rolesList = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .toList();
        claims.put("roles", rolesList);
        List<String> permisosList = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();
        claims.put("permisos", permisosList);
        String rolSingular = rolesList.isEmpty() ? "ROLE_CLIENTE" : rolesList.get(0);
        claims.put("rol", rolSingular);
        claims.put("email", userDetails.getUsername());

        String sub = userDetails.getUsername();
        if (userDetails instanceof CustomUserDetails customUser && customUser.getIdUsuario() != null) {
            sub = customUser.getIdUsuario().toString();
        }

        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .claims(claims)
                .id(jti)
                .subject(sub)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(clave())
                .compact();
    }

    public Claims extraerTodosLosClaims(String token) {
        return parsear(token).getPayload();
    }

    public String extraerUsername(String token) {
        Claims claims = extraerTodosLosClaims(token);
        String email = claims.get("email", String.class);
        return email != null ? email : claims.getSubject();
    }

    public String extraerJti(String token) {
        return extraerTodosLosClaims(token).getId();
    }

    public long extraerTiempoRestante(String token) {
        Date expiracion = extraerTodosLosClaims(token).getExpiration();
        long restante = expiracion.getTime() - System.currentTimeMillis();
        return Math.max(0, restante);
    }

    public boolean esValido(String token, UserDetails userDetails) {
        String username = extraerUsername(token);
        Date expiracion = parsear(token).getPayload().getExpiration();
        return username.equals(userDetails.getUsername()) && expiracion.after(new Date()) && !esRefreshToken(token);
    }

    public boolean esRefreshTokenValido(String token, UserDetails userDetails) {
        String username = extraerUsername(token);
        Date expiracion = parsear(token).getPayload().getExpiration();
        return username.equals(userDetails.getUsername()) && expiracion.after(new Date()) && esRefreshToken(token);
    }

    public boolean esRefreshToken(String token) {
        try {
            return "refresh".equals(extraerTodosLosClaims(token).get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    public String generarRefreshToken(UserDetails userDetails) {
        String sub = userDetails.getUsername();
        if (userDetails instanceof CustomUserDetails customUser && customUser.getIdUsuario() != null) {
            sub = customUser.getIdUsuario().toString();
        }
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .claim("type", "refresh")
                .claim("email", userDetails.getUsername())
                .id(jti)
                .subject(sub)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpirationMs))
                .signWith(clave())
                .compact();
    }

    private Jws<Claims> parsear(String token) {
        return Jwts.parser()
                .verifyWith(clave())
                .build()
                .parseSignedClaims(token);
    }
}
