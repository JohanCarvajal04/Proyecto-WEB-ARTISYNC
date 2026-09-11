package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.ViolationResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.service.comunicacion.ViolationService;

/**
 * Controlador de administración de infracciones y suspensiones.
 * RF-15: exige el permiso INFRACCION_GESTIONAR (V10__permisos_navegacion.sql),
 * que en el seed solo tiene ADMIN pero es asignable a cualquier rol.
 */
@Tag(name = "Admin — Infracciones", description = "Gestión de infracciones y suspensiones de cuenta (INFRACCION_GESTIONAR)")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminViolationController {

    private final ViolationService infraccionService;

    /**
     * Lista todas las infracciones registradas en el sistema.
     *
     * @param pageable configuración de paginación
     * @return página con las infracciones del sistema
     */
    @Operation(summary = "Listar todas las infracciones del sistema")
    @GetMapping("/infracciones")
    @PreAuthorize("hasAuthority('INFRACCION_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<ViolationResponse>> listarInfracciones(Pageable pageable) {
        return ResponseEntity.ok(infraccionService.listarInfracciones(pageable));
    }

    /**
     * Obtiene el historial de infracciones de un usuario específico.
     *
     * @param idUsuario identificador del usuario
     * @param pageable configuración de paginación
     * @return página con el historial de infracciones del usuario
     */
    @Operation(summary = "Historial de infracciones de un usuario específico")
    @GetMapping("/infracciones/usuario/{idUsuario}")
    @PreAuthorize("hasAuthority('INFRACCION_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<Page<ViolationResponse>> historialPorUsuario(
            @PathVariable Long idUsuario,
            Pageable pageable) {
        return ResponseEntity.ok(infraccionService.historialPorUsuario(idUsuario, pageable));
    }

    /**
     * Revierte la suspensión de la cuenta de un usuario.
     *
     * @param idUsuario identificador del usuario cuya suspensión se revierte
     * @return mensaje de confirmación de la reactivación
     * @throws ResourceNotFoundException si el usuario no existe
     */
    @Operation(summary = "Revertir suspensión de un usuario")
    @DeleteMapping("/suspensiones/{idUsuario}")
    @PreAuthorize("hasAuthority('INFRACCION_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> revertirSuspension(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(infraccionService.revertirSuspension(idUsuario));
    }
}
