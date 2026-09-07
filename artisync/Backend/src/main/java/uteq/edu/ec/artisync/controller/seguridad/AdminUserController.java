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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.seguridad.FiltroUsuario;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.seguridad.AdminUserService;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.RespuestaDocumento;

@RestController
@RequestMapping("/api/v1/admin/usuarios")
@RequiredArgsConstructor
@Tag(name = "Administración de Usuarios", description = "Endpoints protegidos para administración completa de usuarios (CRUD con seguridad granular)")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * Lista todos los usuarios de forma paginada, con filtros opcionales.
     *
     * @param filtro criterios opcionales para filtrar el listado de usuarios
     * @param page número de página solicitada (base 0)
     * @param size tamaño de la página
     * @param sortBy campo por el cual ordenar el resultado
     * @param direction dirección del orden ("asc" o "desc")
     * @return página con los usuarios que cumplen el filtro
     */
    @Operation(summary = "Listar todos los usuarios paginados")
    @GetMapping
    @PreAuthorize("hasAuthority('USUARIO_VER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<UserResponse>> getAllUsers(
            FiltroUsuario filtro,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "idUsuario") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort sort = direction.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(adminUserService.getAllUsers(filtro, pageable));
    }

    /**
     * Exporta el listado de usuarios filtrado en el formato solicitado.
     *
     * @param filtro criterios opcionales para filtrar el listado de usuarios
     * @param formato formato del documento a generar (CSV, XLSX o PDF)
     * @param authentication autenticación del administrador que solicita la exportación
     * @return el documento generado con el listado de usuarios
     */
    @Operation(summary = "Exportar el listado de usuarios en CSV, XLSX o PDF")
    @GetMapping("/exportar")
    @PreAuthorize("hasAuthority('USUARIO_EXPORTAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportar(FiltroUsuario filtro, @RequestParam FormatoReporte formato,
                                            Authentication authentication) {
        DocumentoGenerado documento = adminUserService.exportar(filtro, formato, authentication.getName());
        return RespuestaDocumento.de(documento);
    }

    /**
     * Obtiene el detalle de un usuario por su identificador.
     *
     * @param id identificador del usuario
     * @return el usuario solicitado
     */
    @Operation(summary = "Obtener usuario por ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_VER') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.getUserById(id));
    }

    /**
     * Crea un nuevo usuario desde el panel administrativo.
     *
     * @param request datos del usuario a crear
     * @return el usuario creado, con estado 201
     */
    @Operation(summary = "Crear nuevo usuario desde el panel administrativo")
    @PostMapping
    @PreAuthorize("hasAuthority('USUARIO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminUserService.createUser(request));
    }

    /**
     * Actualiza la información y configuración de un usuario.
     *
     * @param id identificador del usuario a actualizar
     * @param request datos actualizados del usuario
     * @return el usuario actualizado
     */
    @Operation(summary = "Actualizar información y configuración de un usuario")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody AdminUpdateUserRequest request) {
        return ResponseEntity.ok(adminUserService.updateUser(id, request));
    }

    /**
     * Activa, desactiva o suspende la cuenta de un usuario.
     *
     * @param id identificador del usuario
     * @param request nuevo estado a aplicar sobre la cuenta
     * @param userDetails administrador autenticado que realiza el cambio
     * @return el usuario con su estado actualizado
     * @throws ExcepcionReglaNegocio si el administrador intenta desactivar su propia cuenta
     */
    @Operation(summary = "Activar o desactivar cuenta de un usuario (Soft Delete / Suspensión)")
    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('USUARIO_SUSPENDER') or hasAuthority('USUARIO_ELIMINAR') or hasAuthority('USUARIO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> changeEstado(@PathVariable Long id, @Valid @RequestBody ChangeEstadoRequest request,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(adminUserService.changeEstado(id, request, userDetails.getIdUsuario()));
    }

    /**
     * Asigna roles a un usuario.
     *
     * @param id identificador del usuario
     * @param request roles a asignar
     * @param userDetails administrador autenticado que realiza la asignación
     * @return el usuario con sus roles actualizados
     * @throws ExcepcionReglaNegocio si el administrador intenta cambiar sus propios roles
     */
    @Operation(summary = "Asignar roles a un usuario")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('ROL_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<UserResponse> assignRoles(@PathVariable Long id, @Valid @RequestBody AssignRolesRequest request,
                                                     @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(adminUserService.assignRoles(id, request, userDetails.getIdUsuario()));
    }

    /**
     * Revoca inmediatamente todas las sesiones activas de un usuario.
     *
     * @param id identificador del usuario cuyas sesiones se revocan
     * @return mensaje de confirmación de la revocación
     */
    @Operation(summary = "Revocar inmediatamente todas las sesiones activas de un usuario")
    @DeleteMapping("/{id}/sesiones")
    @PreAuthorize("hasAuthority('SESION_REVOCAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> revokeUserSessions(@PathVariable Long id) {
        return ResponseEntity.ok(adminUserService.revokeUserSessions(id));
    }

    /**
     * Elimina lógicamente a un usuario (soft delete).
     *
     * @param id identificador del usuario a eliminar
     * @param userDetails administrador autenticado que solicita la eliminación
     * @return respuesta vacía con estado 204
     * @throws ExcepcionReglaNegocio si el administrador intenta eliminar su propia cuenta
     */
    @Operation(summary = "Eliminar lógicamente a un usuario (Soft Delete)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_ELIMINAR') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id, @AuthenticationPrincipal CustomUserDetails userDetails) {
        adminUserService.deleteUser(id, userDetails.getIdUsuario());
        return ResponseEntity.noContent().build();
    }
}

