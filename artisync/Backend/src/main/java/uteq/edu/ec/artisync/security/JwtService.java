package uteq.edu.ec.artisync.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
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

    private static final String EMISOR = "artisync-backend";
    private static final String AUDIENCIA = "artisync-frontend";
    private static final String TIPO_ACCESO = "access";
    private static final String TIPO_REFRESH = "refresh";

    /** RFC 7518 §3.2: HS256 exige una clave de al menos 256 bits (32 bytes). */
    private static final int LONGITUD_MINIMA_CLAVE_BYTES = 32;
    private static final long TOLERANCIA_RELOJ_SEGUNDOS = 60;

    private final SecretKey clave;
    private final JwtParser parser;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtService(
            @Value("${security.jwt.secret-key}") String secret,
            @Value("${security.jwt.expiration-time:86400000}") long expirationMs,
            @Value("${security.jwt.refresh-expiration-time:604800000}") long refreshExpirationMs) {

        byte[] material = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (material.length < LONGITUD_MINIMA_CLAVE_BYTES) {
            throw new IllegalStateException(
                    "security.jwt.secret-key debe tener al menos " + LONGITUD_MINIMA_CLAVE_BYTES
                            + " bytes (256 bits) para HS256; se recibieron " + material.length
                            + ". Generar uno nuevo con: openssl rand -hex 32");
        }

        this.clave = Keys.hmacShaKeyFor(material);
        this.parser = Jwts.parser()
                .verifyWith(this.clave)
                .requireIssuer(EMISOR)
                .requireAudience(AUDIENCIA)
                .clockSkewSeconds(TOLERANCIA_RELOJ_SEGUNDOS)
                .build();
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
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
        Date ahora = new Date();

        return Jwts.builder()
                .claims(claims)
                .id(jti)
                .subject(sub)
                .issuer(EMISOR)
                .audience().add(AUDIENCIA).and()
                .claim("type", TIPO_ACCESO)
                .notBefore(ahora)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + expirationMs))
                .signWith(clave)
                .compact();
    }

    public String generarRefreshToken(UserDetails userDetails) {
        String sub = userDetails.getUsername();
        if (userDetails instanceof CustomUserDetails customUser && customUser.getIdUsuario() != null) {
            sub = customUser.getIdUsuario().toString();
        }
        String jti = UUID.randomUUID().toString();
        Date ahora = new Date();

        return Jwts.builder()
                .claim("type", TIPO_REFRESH)
                .claim("email", userDetails.getUsername())
                .id(jti)
                .subject(sub)
                .issuer(EMISOR)
                .audience().add(AUDIENCIA).and()
                .notBefore(ahora)
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + refreshExpirationMs))
                .signWith(clave)
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

    /**
     * Valida un access token de forma completa: firma, issuer, audience, tolerancia
     * de reloj (todo a cargo del {@link #parser}), tipo (allowlist: solo "access"),
     * titular y que la cuenta siga habilitada y no bloqueada (§2.4 — OBS-AUTO-05).
     */
    public boolean esAccessTokenValido(String token, UserDetails userDetails) {
        try {
            Claims claims = parsear(token).getPayload();
            if (!TIPO_ACCESO.equals(claims.get("type"))) {
                return false;
            }
            String email = claims.get("email", String.class);
            String username = email != null ? email : claims.getSubject();
            return username != null
                    && username.equals(userDetails.getUsername())
                    && userDetails.isEnabled()
                    && userDetails.isAccountNonLocked();
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public boolean esRefreshTokenValido(String token, UserDetails userDetails) {
        String username = extraerUsername(token);
        Date expiracion = parsear(token).getPayload().getExpiration();
        return username.equals(userDetails.getUsername())
                && expiracion.after(new Date())
                && esRefreshToken(token)
                && userDetails.isEnabled()
                && userDetails.isAccountNonLocked();
    }

    public boolean esRefreshToken(String token) {
        try {
            return TIPO_REFRESH.equals(extraerTodosLosClaims(token).get("type"));
        } catch (Exception e) {
            return false;
        }
    }

    private Jws<Claims> parsear(String token) {
        return parser.parseSignedClaims(token);
    }
}
