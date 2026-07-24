package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaInfraccion;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.service.comunicacion.InfraccionService;

/**
 * Controlador de administración de infracciones y suspensiones.
 * RF-15: Solo ADMIN puede consultar historial y revertir suspensiones.
 */
@Tag(name = "Admin — Infracciones", description = "Gestión de infracciones y suspensiones de cuenta (solo ADMIN)")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminInfraccionControlador {

    private final InfraccionService infraccionService;

    @Operation(summary = "Listar todas las infracciones del sistema")
    @GetMapping("/infracciones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RespuestaInfraccion>> listarInfracciones(Pageable pageable) {
        return ResponseEntity.ok(infraccionService.listarInfracciones(pageable));
    }

    @Operation(summary = "Historial de infracciones de un usuario específico")
    @GetMapping("/infracciones/usuario/{idUsuario}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<RespuestaInfraccion>> historialPorUsuario(
            @PathVariable Long idUsuario,
            Pageable pageable) {
        return ResponseEntity.ok(infraccionService.historialPorUsuario(idUsuario, pageable));
    }

    @Operation(summary = "Revertir suspensión de un usuario")
    @DeleteMapping("/suspensiones/{idUsuario}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RespuestaMensaje> revertirSuspension(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(infraccionService.revertirSuspension(idUsuario));
    }
}
