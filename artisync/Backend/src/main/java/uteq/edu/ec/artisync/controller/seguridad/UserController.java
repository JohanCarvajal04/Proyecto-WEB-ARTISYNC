package uteq.edu.ec.artisync.controller.seguridad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.seguridad.request.ChangePasswordRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateUserRequest;
import uteq.edu.ec.artisync.dto.shared.MessageResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.service.seguridad.UserService;

import java.security.Principal;

@RestController
@RequestMapping({"/api/usuarios", "/api/users"})
@RequiredArgsConstructor
@Tag(name = "Gestión de Usuarios", description = "Endpoints protegidos para administración del perfil del usuario actual")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @Operation(summary = "Obtener el perfil completo del usuario autenticado actual")
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Principal principal) {
        return ResponseEntity.ok(userService.getCurrentUser(principal.getName()));
    }

    @Operation(summary = "Actualizar información personal del usuario autenticado actual")
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateCurrentUser(Principal principal, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateCurrentUser(principal.getName(), request));
    }

    @Operation(summary = "Cambiar la contraseña del usuario autenticado actual")
    @PutMapping("/me/password")
    public ResponseEntity<MessageResponse> changePassword(Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(userService.changePassword(principal.getName(), request));
    }

    @Operation(summary = "Desactivar la cuenta del usuario autenticado actual")
    @DeleteMapping("/me")
    public ResponseEntity<MessageResponse> deleteOwnAccount(Principal principal) {
        return ResponseEntity.ok(userService.deleteOwnAccount(principal.getName()));
    }

    @Operation(summary = "Cerrar todas las sesiones activas del usuario actual")
    @DeleteMapping("/me/sesiones")
    public ResponseEntity<MessageResponse> revokeAllMySessions(Principal principal) {
        return ResponseEntity.ok(userService.revokeAllMySessions(principal.getName()));
    }
}
