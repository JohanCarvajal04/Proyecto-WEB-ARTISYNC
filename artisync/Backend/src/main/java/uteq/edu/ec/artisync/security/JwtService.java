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

/**
 * Componente de Seguridad: Offering criptografico para tokens JWT.
 * 
 * Propósito: Centralizar la logica de generacion, validacion, firma y extraccion de claims de los JSON Web Tokens.
 * 
 * Flujo interno: Utiliza el algoritmo de firma provisto en las variables de entorno. Provee validacion de expiracion y encapsula el ID del usuario de forma segura.
 */
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

    /**
     * Construye el servicio y valida la clave de firma antes de arrancar.
     *
     * @param secret clave HS256 en texto plano ({@code security.jwt.secret-key})
     * @param expirationMs vigencia del access token en milisegundos
     * @param refreshExpirationMs vigencia del refresh token en milisegundos
     * @throws IllegalStateException si {@code secret} tiene menos de 32 bytes (256 bits),
     *         el mínimo que exige HS256 — evita arrancar con una clave débil
     */
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

    /**
     * @return la vigencia configurada del access token, en milisegundos
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * @return la vigencia configurada del refresh token, en milisegundos
     */
    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    /**
     * Genera un access token ({@code type=access}) firmado HS256, con los roles,
     * permisos y rol singular del usuario embebidos como claims para que el
     * frontend no necesite otra llamada para conocer sus privilegios.
     *
     * @param userDetails usuario autenticado; si es un {@link CustomUserDetails}
     *                     con {@code idUsuario}, el subject del token es ese id
     *                     (no el correo), para no exponerlo en el JWT decodificado
     * @return el JWT compacto firmado, con vigencia {@link #expirationMs}
     */
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

    /**
     * Genera un refresh token ({@code type=refresh}) firmado HS256, deliberadamente
     * sin roles ni permisos embebidos (un refresh token nunca autentica peticiones
     * de negocio por sí solo, ver {@link JwtAuthenticationFilter}, que solo acepta
     * {@code type=access}).
     *
     * @param userDetails usuario autenticado; mismo criterio de subject que {@link #generarToken}
     * @return el JWT compacto firmado, con vigencia {@link #refreshExpirationMs}
     */
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

    /**
     * Extrae y valida (firma, issuer, audience, tolerancia de reloj) todos los
     * claims del token.
     *
     * @param token JWT compacto firmado
     * @return los claims del token
     * @throws io.jsonwebtoken.JwtException si el token está expirado, mal firmado
     *         o no cumple issuer/audience; {@link IllegalArgumentException} si está
     *         vacío o mal formado
     */
    public Claims extraerTodosLosClaims(String token) {
        return parsear(token).getPayload();
    }

    /**
     * @param token JWT compacto firmado
     * @return el claim {@code email} si está presente; si no, el subject del token
     */
    public String extraerUsername(String token) {
        Claims claims = extraerTodosLosClaims(token);
        String email = claims.get("email", String.class);
        return email != null ? email : claims.getSubject();
    }

    /**
     * @param token JWT compacto firmado
     * @return el JTI (identificador único) del token, usado como clave de revocación en Redis
     */
    public String extraerJti(String token) {
        return extraerTodosLosClaims(token).getId();
    }

    /**
     * @param token JWT compacto firmado
     * @return milisegundos restantes hasta la expiración del token; nunca negativo
     *         (0 si ya expiró)
     */
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

    /**
     * Valida un refresh token: firma/issuer/audience (vía {@link #parsear}), que
     * no haya expirado, que sea {@code type=refresh}, titular y que la cuenta
     * siga habilitada y no bloqueada.
     *
     * @param token JWT compacto a validar
     * @param userDetails usuario contra el que se compara el titular del token
     * @return {@code true} si el refresh token es válido para ese usuario
     * @throws io.jsonwebtoken.JwtException si el token está mal firmado o no cumple issuer/audience
     */
    public boolean esRefreshTokenValido(String token, UserDetails userDetails) {
        String username = extraerUsername(token);
        Date expiracion = parsear(token).getPayload().getExpiration();
        return username.equals(userDetails.getUsername())
                && expiracion.after(new Date())
                && esRefreshToken(token)
                && userDetails.isEnabled()
                && userDetails.isAccountNonLocked();
    }

    /**
     * @param token JWT compacto a inspeccionar
     * @return {@code true} si el claim {@code type} vale {@code "refresh"};
     *         {@code false} también ante cualquier token inválido o malformado
     *         (nunca propaga la excepción, a diferencia del resto de métodos de extracción)
     */
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

