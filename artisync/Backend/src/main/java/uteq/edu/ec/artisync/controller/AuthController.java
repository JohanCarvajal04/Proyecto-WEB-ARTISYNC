package uteq.edu.ec.artisync.controller;

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
import uteq.edu.ec.artisync.dto.request.*;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.TokenResponse;
import uteq.edu.ec.artisync.dto.response.UserResponse;
import uteq.edu.ec.artisync.service.AuthService;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints públicos para registro (RNF-12), login, 2FA, refresh token y recuperación de contraseña")
public class AuthController {

    @Value("${app.security.cookie-secure:false}")
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
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "Verificar código de autenticación de doble factor (2FA)")
    @PostMapping("/2fa/verify")
    public ResponseEntity<TokenResponse> verify2Fa(@Valid @RequestBody TwoFactorRequest request, HttpServletResponse response) {
        TokenResponse tokenResponse = authService.verify2Fa(request);
        setRefreshTokenCookie(response, tokenResponse.getRefreshToken());
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "Refrescar token de acceso utilizando Refresh Token en cookie HttpOnly o cuerpo JSON")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie,
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
    public ResponseEntity<Void> logout(HttpServletRequest request, @CookieValue(name = "refreshToken", required = false) String refreshTokenCookie, HttpServletResponse response) {
        String authHeader = request.getHeader("Authorization");
        authService.logout(authHeader, refreshTokenCookie);
        clearRefreshTokenCookie(response);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Solicitar enlace/token de recuperación de contraseña")
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @Operation(summary = "Reestablecer contraseña utilizando token válido")
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        if (refreshToken != null) {
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(cookieSecure) // Configurable dinámicamente según entorno (HTTPS en producción)
                    .path("/api/auth")
                    .maxAge(604800) // 7 días en segundos
                    .sameSite("Strict")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(cookieSecure)
                .path("/api/auth")
                .maxAge(0)
                .sameSite("Strict")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
