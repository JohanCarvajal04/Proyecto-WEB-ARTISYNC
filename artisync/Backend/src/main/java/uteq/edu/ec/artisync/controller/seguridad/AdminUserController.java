package uteq.edu.ec.artisync.controller.seguridad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.service.seguridad.AdminUserService;
import uteq.edu.ec.artisync.util.PagedResponse;

@RestController
@RequestMapping({"/api/admin/usuarios", "/api/admin/users"})
@RequiredArgsConstructor
@Tag(name = "Administración de Usuarios", description = "Endpoints protegidos para administración completa de usuarios (CRUD con seguridad granular)")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Listar todos los usuarios paginados")
    @GetMapping
    @PreAuthorize("hasAuthority('USUARIO_VER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idUsuario") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        
        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(adminUserService.getAllUsers(pageable));
    }

    @Operation(summary = "Obtener usuario por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_VER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    @Operation(summary = "Crear nuevo usuario desde el panel administrativo")
    @PostMapping
    @PreAuthorize("hasAuthority('USUARIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
    }

    @Operation(summary = "Actualizar información y configuración de un usuario")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    @Operation(summary = "Activar o desactivar cuenta de un usuario (Soft Delete / Suspensión)")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('USUARIO_SUSPENDER') or hasAuthority('USUARIO_ELIMINAR') or hasAuthority('USUARIO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeEstado(@PathVariable Long id, @Valid @RequestBody ChangeEstadoRequest request) {
        return ResponseEntity.ok(adminUserService.changeEstado(id, request));
    }

    @Operation(summary = "Asignar roles a un usuario")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROL_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request) {
        return ResponseEntity.ok(adminUserService.assignRoles(id, request));
    }

    @Operation(summary = "Revocar inmediatamente todas las sesiones activas de un usuario")
    @DeleteMapping("/{id}/sesiones")
    @PreAuthorize("hasAuthority('SESION_REVOCAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> revokeUserSessions(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.revokeUserSessions(id));
    }

    @Operation(summary = "Eliminar lógicamente a un usuario (Soft Delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_ELIMINAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

