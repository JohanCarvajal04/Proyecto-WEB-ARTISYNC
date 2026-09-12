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
            "test-fake-jwt-secret-do-not-use-in-production-0000000000"; // 56 bytes

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
    void generateToken_ShouldIncludeTypeAccessIssuerAndAudience() {
        String token = jwtService.generateToken(usuario);
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
    void generateRefreshToken_ShouldIncludeTypeRefresh() {
        String token = jwtService.generateRefreshToken(usuario);
        Claims claims = jwtService.extraerTodosLosClaims(token);

        assertEquals("refresh", claims.get("type"));
        assertTrue(jwtService.isRefreshToken(token));
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
    void isAccessTokenValid_ShouldReturnTrue_WhenTokenAndUserMatch() {
        String token = jwtService.generateToken(usuario);
        assertTrue(jwtService.isAccessTokenValid(token, usuario));
    }

    @Test
    void isAccessTokenValid_ShouldReturnFalse_WhenUserDisabled() {
        String token = jwtService.generateToken(usuario);
        CustomUserDetails deshabilitado = new CustomUserDetails(
                1L, "usuario@example.com", "hash-irrelevante", false, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.isAccessTokenValid(token, deshabilitado));
    }

    @Test
    void isAccessTokenValid_ShouldReturnFalse_WhenTypeIsRefresh() {
        String refreshToken = jwtService.generateRefreshToken(usuario);
        assertFalse(jwtService.isAccessTokenValid(refreshToken, usuario));
    }

    @Test
    void isAccessTokenValid_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
        String token = jwtService.generateToken(usuario);
        CustomUserDetails otroUsuario = new CustomUserDetails(
                2L, "otro@example.com", "hash-irrelevante", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.isAccessTokenValid(token, otroUsuario));
    }

    @Test
    void isRefreshTokenValid_ShouldReturnFalse_WhenUserDisabled() {
        String refreshToken = jwtService.generateRefreshToken(usuario);
        CustomUserDetails deshabilitado = new CustomUserDetails(
                1L, "usuario@example.com", "hash-irrelevante", false, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.isRefreshTokenValid(refreshToken, deshabilitado));
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
        String token = jwtService.generateToken(usuario);
        String jti = jwtService.extraerJti(token);

        assertNotNull(jti);
        assertEquals(jwtService.extraerTodosLosClaims(token).getId(), jti);
    }

    @Test
    void extraerTiempoRestante_ShouldReturnPositiveValue_ForFreshToken() {
        String token = jwtService.generateToken(usuario);

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
    void isAccessTokenValid_ShouldReturnFalse_WhenTokenEsMalformado() {
        assertFalse(jwtService.isAccessTokenValid("token-malformado-no-jwt", usuario));
    }

    @Test
    void isAccessTokenValid_ShouldReturnFalse_WhenAccountIsLocked() {
        String token = jwtService.generateToken(usuario);
        CustomUserDetails bloqueado = new CustomUserDetails(
                1L, "usuario@example.com", "hash-irrelevante", true, true, true, false,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.isAccessTokenValid(token, bloqueado));
    }

    @Test
    void isRefreshToken_ShouldReturnFalse_WhenTokenEsMalformado() {
        assertFalse(jwtService.isRefreshToken("token-malformado-no-jwt"));
    }

    @Test
    void isRefreshTokenValid_ShouldReturnTrue_WhenTodoCoincide() {
        String refreshToken = jwtService.generateRefreshToken(usuario);

        assertTrue(jwtService.isRefreshTokenValid(refreshToken, usuario));
    }

    @Test
    void isRefreshTokenValid_ShouldReturnFalse_WhenUsernameNoCoincide() {
        String refreshToken = jwtService.generateRefreshToken(usuario);
        CustomUserDetails otroUsuario = new CustomUserDetails(
                2L, "otro@example.com", "hash-irrelevante", true, true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.isRefreshTokenValid(refreshToken, otroUsuario));
    }

    @Test
    void isRefreshTokenValid_ShouldReturnFalse_WhenTokenEsDeAcceso() {
        String accessToken = jwtService.generateToken(usuario);

        assertFalse(jwtService.isRefreshTokenValid(accessToken, usuario));
    }

    @Test
    void isRefreshTokenValid_ShouldReturnFalse_WhenAccountIsLocked() {
        String refreshToken = jwtService.generateRefreshToken(usuario);
        CustomUserDetails bloqueado = new CustomUserDetails(
                1L, "usuario@example.com", "hash-irrelevante", true, true, true, false,
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));

        assertFalse(jwtService.isRefreshTokenValid(refreshToken, bloqueado));
    }

    @Test
    void isRefreshTokenValid_ShouldReturnFalse_WhenTokenEstaExpiradoDentroDeLaToleranciaDeReloj() {
        // clockSkewSeconds=60 en el parser tolera una expiracion pasada de hasta
        // 60s (no lanza ExpiredJwtException), por eso una expiracion de hace 30s
        // SI llega al chequeo manual expiracion.after(new Date()) de
        // isRefreshTokenValid, que es el que debe devolver false aqui.
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

        assertFalse(jwtService.isRefreshTokenValid(refreshExpirado, usuario));
    }
}
