package uteq.edu.ec.artisync.controller.seguridad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.TokenResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.service.seguridad.AuthService;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints públicos para registro (RNF-12), login, 2FA, refresh token y recuperación de contraseña")
public class AuthController {

    private static final String COOKIE_REFRESH = "refreshToken";
    private static final int MAX_AGE_REFRESH_SEGUNDOS = 604800; // 7 días

    // §2.1 (OBS-AUTO-05): cookie del ticket pre-auth de 2FA — ver
    // PreAuth2faTicketService. El path debe ser "/api/v1/auth" (no el más
    // estrecho "/api/v1/auth/2fa/verify") porque el navegador solo envía una
    // cookie cuyo path es prefijo de la ruta solicitada, y esta cookie
    // también debe poder LIMPIARSE desde /api/v1/auth/login (login sin 2FA) y
    // /api/v1/auth/logout.
    private static final String COOKIE_PRE_AUTH_2FA = "preAuth2fa";
    private static final int MAX_AGE_PRE_AUTH_2FA_SEGUNDOS = 300; // 5 min — igual TTL que en Redis

    // Por defecto true (falla seguro): solo el perfil/entorno de desarrollo local
    // (APP_COOKIE_SECURE=false en .env) lo desactiva para poder probar por HTTP.
    @Value("${app.security.cookie-secure:true}")
    private boolean cookieSecure;

    private final AuthService authService;

    @Operation(summary = "Registrar nuevo usuario con validación de mayoría de edad (RNF-12)")
    @PostMapping("/registro")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse userResponse = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponse);
    }

    @Operation(summary = "Iniciar sesión y obtener token JWT o requerimiento 2FA")
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.login(request);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        setPreAuth2faCookie(response, tokenResponse.getPreAuthTicket());
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "Verificar código de autenticación de doble factor (2FA) usando el ticket pre-auth emitido por /login")
    @PostMapping("/2fa/verify")
    public ResponseEntity<TokenResponse> verify2Fa(
            @CookieValue(name = COOKIE_PRE_AUTH_2FA, required = false) String preAuthTicket,
            @Valid @RequestBody TwoFactorRequest request,
            HttpServletResponse response) {
        if (preAuthTicket == null || preAuthTicket.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        TokenResponse tokenResponse = authService.verify2Fa(preAuthTicket, request);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        clearPreAuth2faCookie(response);
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "Refrescar token de acceso utilizando Refresh Token en cookie HttpOnly o cuerpo JSON")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = COOKIE_REFRESH, required = false) String refreshTokenCookie,
            @RequestBody(required = false) RefreshTokenRequest requestBody,
            HttpServletResponse response) {
        String tokenToRefresh = refreshTokenCookie;
        if (tokenToRefresh == null && requestBody != null) {
            tokenToRefresh = requestBody.getRefreshToken();
        }
        if (tokenToRefresh == null || tokenToRefresh.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        TokenResponse tokenResponse = authService.refreshToken(tokenToRefresh);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "Cerrar sesión e invalidar token JWT y Refresh Token en Redis Blacklist y BD", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, @CookieValue(name = COOKIE_REFRESH, required = false) String refreshTokenCookie, HttpServletResponse response) {
        String authHeader = request.getHeader("Authorization");
        authService.logout(authHeader, refreshTokenCookie);
        clearRefreshTokenCookie(response);
        clearPreAuth2faCookie(response); // limpieza defensiva por si quedó un ticket sin consumir
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Solicitar enlace/token de recuperación de contraseña")
    @PostMapping("/forgot-password")
    public ResponseEntity<RespuestaMensaje> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @Operation(summary = "Reestablecer contraseña utilizando token válido")
    @PostMapping("/reset-password")
    public ResponseEntity<RespuestaMensaje> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (refreshToken != null) {
            escribirCookie(response, COOKIE_REFRESH, refreshToken, MAX_AGE_REFRESH_SEGUNDOS);
        }
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        escribirCookie(response, COOKIE_REFRESH, "", 0);
    }

    private void setPreAuth2faCookie(HttpServletResponse response, String ticket) {
        if (ticket != null) {
            escribirCookie(response, COOKIE_PRE_AUTH_2FA, ticket, MAX_AGE_PRE_AUTH_2FA_SEGUNDOS);
        }
    }

    private void clearPreAuth2faCookie(HttpServletResponse response) {
        escribirCookie(response, COOKIE_PRE_AUTH_2FA, "", 0);
    }

    private void escribirCookie(HttpServletResponse response, String nombre, String valor, int maxAgeSegundos) {
        ResponseCookie cookie = ResponseCookie.from(nombre, valor)
                .httpOnly(true)
                .secure(cookieSecure) // Configurable dinámicamente según entorno (HTTPS en producción)
                .path("/api/v1/auth")
                .maxAge(maxAgeSegundos)
                .sameSite("Lax")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
