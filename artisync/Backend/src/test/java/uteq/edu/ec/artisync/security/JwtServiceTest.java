package uteq.edu.ec.artisync.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * §2.6 / §2.7 (OBS-AUTO-05..07): construye un {@link JwtService} real en lugar de
 * mockearlo — el objetivo es probar la validacion de firma/issuer/audience/reloj
 * que antes no existia, no el comportamiento de un doble de prueba.
 */
class JwtServiceTest {

    private static final String SECRETO_VALIDO =
            "d5d0f9946b0a3804c562579f9ad06d66dd9ed91c4a5d7787cf7a139fd34ad834"; // 64 bytes

    private JwtService jwtService;
    private CustomUserDetails usuario;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRETO_VALIDO, 86_400_000L, 604_800_000L);
        usuario = new CustomUserDetails(
                1L, "usuario@example.com", "hash-irrelevante", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
    }

    @Test
    void constructor_ShouldThrow_WhenSecretShorterThan32Bytes() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtService("secreto-demasiado-corto", 86_400_000L, 604_800_000L));
        assertTrue(ex.getMessage().contains("256 bits"));
    }

    @Test
    void constructor_ShouldThrow_WhenSecretIsNull() {
        assertThrows(IllegalStateException.class,
                () -> new JwtService(null, 86_400_000L, 604_800_000L));
    }

    @Test
    void generarToken_ShouldIncludeTypeAccessIssuerAndAudience() {
        String token = jwtService.generarToken(usuario);
        Claims claims = jwtService.extraerTodosLosClaims(token);

        assertEquals("access", claims.get("type"));
        assertEquals("artisync-backend", claims.getIssuer());
        assertTrue(claims.getAudience().contains("artisync-frontend"));
        assertEquals("usuario@example.com", claims.get("email"));
        assertEquals("1", claims.getSubject());
        assertNotNull(claims.getId());
        assertNotNull(claims.getNotBefore());
        assertNotNull(claims.getIssuedAt());
    }

    @Test
    void generarRefreshToken_ShouldIncludeTypeRefresh() {
        String token = jwtService.generarRefreshToken(usuario);
        Claims claims = jwtService.extraerTodosLosClaims(token);

        assertEquals("refresh", claims.get("type"));
        assertTrue(jwtService.esRefreshToken(token));
    }

    @Test
    void parsear_ShouldRejectTokenWithWrongIssuer() {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO_VALIDO.getBytes(StandardCharsets.UTF_8));
        Date ahora = new Date();
        String tokenConIssuerFalso = Jwts.builder()
                .subject("1")
                .claim("email", "usuario@example.com")
                .claim("type", "access")
                .issuer("otro-emisor")
                .audience().add("artisync-frontend").and()
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + 60_000))
                .signWith(clave)
                .compact();

        assertThrows(io.jsonwebtoken.IncorrectClaimException.class,
                () -> jwtService.extraerTodosLosClaims(tokenConIssuerFalso));
    }

    @Test
    void parsear_ShouldRejectTokenWithWrongAudience() {
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO_VALIDO.getBytes(StandardCharsets.UTF_8));
        Date ahora = new Date();
        String tokenConAudienciaFalsa = Jwts.builder()
                .subject("1")
                .claim("email", "usuario@example.com")
                .claim("type", "access")
                .issuer("artisync-backend")
                .audience().add("otra-app").and()
                .issuedAt(ahora)
                .expiration(new Date(ahora.getTime() + 60_000))
                .signWith(clave)
                .compact();

        assertThrows(io.jsonwebtoken.IncorrectClaimException.class,
                () -> jwtService.extraerTodosLosClaims(tokenConAudienciaFalsa));
    }

    @Test
    void parsear_ShouldAcceptTokenWithinClockSkew() {
        // Token "emitido" 45s en el futuro (nbf) — dentro de la tolerancia de 60s.
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO_VALIDO.getBytes(StandardCharsets.UTF_8));
        Date enElFuturo = new Date(System.currentTimeMillis() + 45_000);
        String token = Jwts.builder()
                .subject("1")
                .claim("email", "usuario@example.com")
                .claim("type", "access")
                .issuer("artisync-backend")
                .audience().add("artisync-frontend").and()
                .notBefore(enElFuturo)
                .issuedAt(new Date())
                .expiration(new Date(enElFuturo.getTime() + 60_000))
                .signWith(clave)
                .compact();

        assertDoesNotThrow(() -> jwtService.extraerTodosLosClaims(token));
    }

    @Test
    void esAccessTokenValido_ShouldReturnTrue_WhenTokenAndUserMatch() {
        String token = jwtService.generarToken(usuario);
        assertTrue(jwtService.esAccessTokenValido(token, usuario));
    }

    @Test
    void esAccessTokenValido_ShouldReturnFalse_WhenUserDisabled() {
        String token = jwtService.generarToken(usuario);
        CustomUserDetails deshabilitado = new CustomUserDetails(
                1L, "usuario@example.com", "hash-irrelevante", false, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.esAccessTokenValido(token, deshabilitado));
    }

    @Test
    void esAccessTokenValido_ShouldReturnFalse_WhenTypeIsRefresh() {
        String refreshToken = jwtService.generarRefreshToken(usuario);
        assertFalse(jwtService.esAccessTokenValido(refreshToken, usuario));
    }

    @Test
    void esAccessTokenValido_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
        String token = jwtService.generarToken(usuario);
        CustomUserDetails otroUsuario = new CustomUserDetails(
                2L, "otro@example.com", "hash-irrelevante", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.esAccessTokenValido(token, otroUsuario));
    }

    @Test
    void esRefreshTokenValido_ShouldReturnFalse_WhenUserDisabled() {
        String refreshToken = jwtService.generarRefreshToken(usuario);
        CustomUserDetails deshabilitado = new CustomUserDetails(
                1L, "usuario@example.com", "hash-irrelevante", false, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.esRefreshTokenValido(refreshToken, deshabilitado));
    }

    @Test
    void getExpirationMs_ShouldReturnConfiguredValue() {
        assertEquals(86_400_000L, jwtService.getExpirationMs());
    }

    @Test
    void getRefreshExpirationMs_ShouldReturnConfiguredValue() {
        assertEquals(604_800_000L, jwtService.getRefreshExpirationMs());
    }

    @Test
    void extraerJti_ShouldReturnTokenId() {
        String token = jwtService.generarToken(usuario);
        String jti = jwtService.extraerJti(token);

        assertNotNull(jti);
        assertEquals(jwtService.extraerTodosLosClaims(token).getId(), jti);
    }

    @Test
    void extraerTiempoRestante_ShouldReturnPositiveValue_ForFreshToken() {
        String token = jwtService.generarToken(usuario);

        long restante = jwtService.extraerTiempoRestante(token);

        assertTrue(restante > 0 && restante <= 86_400_000L);
    }

    @Test
    void extraerTiempoRestante_ShouldReturnZero_ForExpiredToken() {
        // clockSkewSeconds=60 en el parser tolera hasta 60s de expiracion pasada
        // (no lanza ExpiredJwtException); 30s de margen alcanza para que
        // Math.max(0, restante) sea el 0 que se ejercita aqui.
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO_VALIDO.getBytes(StandardCharsets.UTF_8));
        Date hace30Segundos = new Date(System.currentTimeMillis() - 30_000);
        String tokenExpirado = Jwts.builder()
                .subject("1")
                .claim("email", "usuario@example.com")
                .claim("type", "access")
                .issuer("artisync-backend")
                .audience().add("artisync-frontend").and()
                .issuedAt(hace30Segundos)
                .expiration(hace30Segundos)
                .signWith(clave)
                .compact();

        assertEquals(0L, jwtService.extraerTiempoRestante(tokenExpirado));
    }

    @Test
    void esAccessTokenValido_ShouldReturnFalse_WhenTokenEsMalformado() {
        assertFalse(jwtService.esAccessTokenValido("token-malformado-no-jwt", usuario));
    }

    @Test
    void esAccessTokenValido_ShouldReturnFalse_WhenAccountIsLocked() {
        String token = jwtService.generarToken(usuario);
        CustomUserDetails bloqueado = new CustomUserDetails(
                1L, "usuario@example.com", "hash-irrelevante", true, true, true, false,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.esAccessTokenValido(token, bloqueado));
    }

    @Test
    void esRefreshToken_ShouldReturnFalse_WhenTokenEsMalformado() {
        assertFalse(jwtService.esRefreshToken("token-malformado-no-jwt"));
    }

    @Test
    void esRefreshTokenValido_ShouldReturnTrue_WhenTodoCoincide() {
        String refreshToken = jwtService.generarRefreshToken(usuario);

        assertTrue(jwtService.esRefreshTokenValido(refreshToken, usuario));
    }

    @Test
    void esRefreshTokenValido_ShouldReturnFalse_WhenUsernameNoCoincide() {
        String refreshToken = jwtService.generarRefreshToken(usuario);
        CustomUserDetails otroUsuario = new CustomUserDetails(
                2L, "otro@example.com", "hash-irrelevante", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.esRefreshTokenValido(refreshToken, otroUsuario));
    }

    @Test
    void esRefreshTokenValido_ShouldReturnFalse_WhenTokenEsDeAcceso() {
        String accessToken = jwtService.generarToken(usuario);

        assertFalse(jwtService.esRefreshTokenValido(accessToken, usuario));
    }

    @Test
    void esRefreshTokenValido_ShouldReturnFalse_WhenAccountIsLocked() {
        String refreshToken = jwtService.generarRefreshToken(usuario);
        CustomUserDetails bloqueado = new CustomUserDetails(
                1L, "usuario@example.com", "hash-irrelevante", true, true, true, false,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.esRefreshTokenValido(refreshToken, bloqueado));
    }

    @Test
    void esRefreshTokenValido_ShouldReturnFalse_WhenTokenEstaExpiradoDentroDeLaToleranciaDeReloj() {
        // clockSkewSeconds=60 en el parser tolera una expiracion pasada de hasta
        // 60s (no lanza ExpiredJwtException), por eso una expiracion de hace 30s
        // SI llega al chequeo manual expiracion.after(new Date()) de
        // esRefreshTokenValido, que es el que debe devolver false aqui.
        SecretKey clave = Keys.hmacShaKeyFor(SECRETO_VALIDO.getBytes(StandardCharsets.UTF_8));
        Date hace30Segundos = new Date(System.currentTimeMillis() - 30_000);
        String refreshExpirado = Jwts.builder()
                .claim("type", "refresh")
                .claim("email", "usuario@example.com")
                .subject("1")
                .issuer("artisync-backend")
                .audience().add("artisync-frontend").and()
                .issuedAt(hace30Segundos)
                .expiration(hace30Segundos)
                .signWith(clave)
                .compact();

        assertFalse(jwtService.esRefreshTokenValido(refreshExpirado, usuario));
    }
}
