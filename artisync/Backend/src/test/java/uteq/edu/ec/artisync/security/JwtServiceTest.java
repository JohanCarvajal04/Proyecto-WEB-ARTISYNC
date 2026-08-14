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
            "f98cf546c1a89c93f0b2f1559868779b76c8c4a4f89d0b676a74c431d1d8ef3f"; // 64 bytes

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
}
