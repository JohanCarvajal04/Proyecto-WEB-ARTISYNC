package uteq.edu.ec.artisync.controller;

import uteq.edu.ec.artisync.dto.request.*;
import uteq.edu.ec.artisync.dto.response.*;
import uteq.edu.ec.artisync.security.UsuarioPrincipal;
import uteq.edu.ec.artisync.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * POST /api/auth/login
     * Autentica usuario y devuelve JWT
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/registro
     * Registra un nuevo usuario
     */
    @PostMapping("/registro")
    public ResponseEntity<UsuarioResponse> registro(
            @Valid @RequestBody RegistroRequest request) {
        UsuarioResponse response = authService.registro(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/auth/logout
     * Cierra sesión invalidando el token en BD
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MensajeResponse> logout(HttpServletRequest request) {
        String token = parseJwt(request);
        MensajeResponse response = authService.logout(token);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/recuperar-password
     * Solicita token de recuperación (envía email)
     */
    @PostMapping("/recuperar-password")
    public ResponseEntity<MensajeResponse> solicitarRecuperacion(
            @RequestParam String correo) {
        MensajeResponse response = authService.solicitarRecuperacion(correo);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/cambiar-password
     * Cambia password con token de recuperación
     */
    @PostMapping("/cambiar-password")
    public ResponseEntity<MensajeResponse> cambiarPassword(
            @Valid @RequestBody CambioPasswordRequest request) {
        MensajeResponse response = authService.cambiarPassword(request);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/auth/perfil
     * Obtiene perfil del usuario autenticado
     */
    @GetMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UsuarioResponse> obtenerPerfil(
            @AuthenticationPrincipal UsuarioPrincipal principal) {
        UsuarioResponse response = authService.obtenerPerfil(principal.getCorreo());
        return ResponseEntity.ok(response);
    }

    private String parseJwt(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
