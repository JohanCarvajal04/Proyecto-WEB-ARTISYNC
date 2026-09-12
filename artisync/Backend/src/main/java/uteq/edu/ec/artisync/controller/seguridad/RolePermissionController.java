package uteq.edu.ec.artisync.controller.seguridad;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.PermissionResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.RoleResponse;
import uteq.edu.ec.artisync.service.seguridad.RolePermissionService;

import java.util.List;

/** Consulta y sincronización de la matriz de permisos por rol. */
@RestController
@RequestMapping("/api/v1/admin/role-permissions")
@RequiredArgsConstructor
@Tag(name = "Administración de Roles y Permisos", description = "Endpoints para consultar y sincronizar dinámicamente la matriz de permisos por rol")
@SecurityRequirement(name = "bearerAuth")
public class RolePermissionController {

    private final RolePermissionService service;

    /**
     * Lista todos los roles del sistema junto con sus permisos asignados.
     *
     * @return listado de roles con sus permisos
     */
    @Operation(summary = "Listar todos los roles y sus permisos asignados")
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROL_VER') or hasAuthority('ROL_GESTIONAR') or hasAuthority('ROL_ASIGNAR_PERMISO') or hasRole('ADMIN')")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(service.getAllRoles());
    }

    /**
     * Lista el catálogo completo de permisos disponibles, agrupados por módulo.
     *
     * @return listado de permisos disponibles
     */
    @Operation(summary = "Listar el catálogo completo de permisos disponibles por módulo")
    @GetMapping("/permisos")
    @PreAuthorize("hasAuthority('PERMISO_VER') or hasAuthority('ROL_GESTIONAR') or hasAuthority('ROL_ASIGNAR_PERMISO') or hasRole('ADMIN')")
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        return ResponseEntity.ok(service.getAllPermissions());
    }

    /**
     * Obtiene los códigos de los permisos asignados a un rol específico.
     *
     * @param roleName nombre del rol
     * @return listado de códigos de permisos asignados al rol
     */
    @Operation(summary = "Obtener lista de códigos de permisos asignados a un rol específico")
    @GetMapping("/{roleName}")
    @PreAuthorize("hasAuthority('ROL_VER') or hasAuthority('PERMISO_VER') or hasAuthority('ROL_GESTIONAR') or hasAuthority('ROL_ASIGNAR_PERMISO') or hasRole('ADMIN')")
    public ResponseEntity<List<String>> getPermissionsByRole(@PathVariable String roleName) {
        return ResponseEntity.ok(service.getPermissionsByRole(roleName));
    }

    /**
     * Sincroniza transaccionalmente la lista completa de permisos asignados a un rol.
     *
     * @param request nombre del rol y lista completa de códigos de permisos a asignar
     * @return mensaje de confirmación de la sincronización
     */
    @Operation(summary = "Sincronizar la lista de permisos de un rol transaccionalmente")
    @PutMapping("/sync")
    @PreAuthorize("hasAuthority('ROL_ASIGNAR_PERMISO') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> syncPermissions(@Valid @RequestBody SyncPermissionsRequest request) {
        service.syncPermissions(request.getRoleName(), request.getPermissionCodes());
        return ResponseEntity.ok(new RespuestaMensaje("Permisos sincronizados con éxito para el rol: " + request.getRoleName()));
    }

    /**
     * Crea un nuevo rol personalizado con su matriz inicial de permisos.
     *
     * @param request datos del rol a crear, incluyendo sus permisos iniciales
     * @return el rol creado, con estado 201
     */
    @Operation(summary = "Crear un nuevo rol personalizado con su matriz inicial de permisos")
    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROL_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(service.createRole(request));
    }

    /**
     * Actualiza la descripción de un rol.
     *
     * @param idRol identificador del rol a actualizar
     * @param request datos actualizados del rol
     * @return el rol actualizado
     */
    @Operation(summary = "Actualizar descripción de un rol")
    @PutMapping("/roles/{idRol}")
    @PreAuthorize("hasAuthority('ROL_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long idRol, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(service.updateRole(idRol, request));
    }

    /**
     * Elimina un rol personalizado.
     *
     * @param idRol identificador del rol a eliminar
     * @return mensaje de confirmación de la eliminación
     */
    @Operation(summary = "Eliminar un rol personalizado")
    @DeleteMapping("/roles/{idRol}")
    @PreAuthorize("hasAuthority('ROL_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> deleteRole(@PathVariable Long idRol) {
        service.deleteRole(idRol);
        return ResponseEntity.ok(new RespuestaMensaje("Role eliminado exitosamente"));
    }
}

