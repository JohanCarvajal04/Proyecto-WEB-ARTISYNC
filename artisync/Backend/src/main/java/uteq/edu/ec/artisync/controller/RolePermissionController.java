package uteq.edu.ec.artisync.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.request.SyncPermissionsRequest;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.PermisoResponse;
import uteq.edu.ec.artisync.dto.response.RolResponse;
import uteq.edu.ec.artisync.service.RolePermissionService;

import java.util.List;

@RestController
@RequestMapping("/api/admin/role-permissions")
@RequiredArgsConstructor
@Tag(name = "Administración de Roles y Permisos", description = "Endpoints para consultar y sincronizar dinámicamente la matriz de permisos por rol")
@SecurityRequirement(name = "bearerAuth")
public class RolePermissionController {

    private final RolePermissionService service;

    @Operation(summary = "Listar todos los roles y sus permisos asignados")
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROL_VER') or hasAuthority('ROL_GESTIONAR') or hasAuthority('ROL_ASIGNAR_PERMISO') or hasRole('ADMIN')")
    public ResponseEntity<List<RolResponse>> getAllRoles() {
        return ResponseEntity.ok(service.getAllRoles());
    }

    @Operation(summary = "Listar el catálogo completo de permisos disponibles por módulo")
    @GetMapping("/permisos")
    @PreAuthorize("hasAuthority('PERMISO_VER') or hasAuthority('ROL_GESTIONAR') or hasAuthority('ROL_ASIGNAR_PERMISO') or hasRole('ADMIN')")
    public ResponseEntity<List<PermisoResponse>> getAllPermisos() {
        return ResponseEntity.ok(service.getAllPermisos());
    }

    @Operation(summary = "Obtener lista de códigos de permisos asignados a un rol específico")
    @GetMapping("/{roleName}")
    @PreAuthorize("hasAuthority('ROL_VER') or hasAuthority('PERMISO_VER') or hasAuthority('ROL_GESTIONAR') or hasAuthority('ROL_ASIGNAR_PERMISO') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getPermissionsByRole(@PathVariable String roleName) {
        return ResponseEntity.ok(service.getPermissionsByRole(roleName));
    }

    @Operation(summary = "Sincronizar la lista de permisos de un rol transaccionalmente")
    @PutMapping("/sync")
    @PreAuthorize("hasAuthority('ROL_ASIGNAR_PERMISO') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> syncPermissions(@Valid @RequestBody SyncPermissionsRequest request) {
        service.syncPermissions(request.getRoleName(), request.getPermissionCodes());
        return ResponseEntity.ok(new MessageResponse("Permisos sincronizados con éxito para el rol: " + request.getRoleName()));
    }

    @Operation(summary = "Crear un nuevo rol personalizado con su matriz inicial de permisos")
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROL_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RolResponse> createRole(@Valid @RequestBody uteq.edu.ec.artisync.dto.request.CreateRoleRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(service.createRole(request));
    }

    @Operation(summary = "Actualizar descripción de un rol")
    @PutMapping("/roles/{idRol}")
    @PreAuthorize("hasAuthority('ROL_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RolResponse> updateRole(@PathVariable Long idRol, @Valid @RequestBody uteq.edu.ec.artisync.dto.request.UpdateRoleRequest request) {
        return ResponseEntity.ok(service.updateRole(idRol, request));
    }

    @Operation(summary = "Eliminar un rol personalizado")
    @DeleteMapping("/roles/{idRol}")
    @PreAuthorize("hasAuthority('ROL_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteRole(@PathVariable Long idRol) {
        service.deleteRole(idRol);
        return ResponseEntity.ok(new MessageResponse("Rol eliminado exitosamente"));
    }
}
