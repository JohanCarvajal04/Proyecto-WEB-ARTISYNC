package uteq.edu.ec.artisync.controller.social;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionActualizarSorteo;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearSorteo;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaGanador;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaParticipante;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaSorteo;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.social.SorteoService;

import java.util.List;

/**
 * Controlador de sorteos configurables.
 * RF-23: CRUD, participación y consulta de ganadores.
 */
@Tag(name = "Sorteos", description = "Gestión de sorteos configurables por el creador")
@RestController
@RequiredArgsConstructor
public class SorteoControlador {

    private final SorteoService sorteoService;

    // =========================================================================
    // CRUD de Sorteos
    // =========================================================================

    @Operation(summary = "Crear un nuevo sorteo (CREADOR)")
    @PostMapping("/api/v1/sorteos")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RespuestaSorteo> crearSorteo(
            @Valid @RequestBody PeticionCrearSorteo peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sorteoService.crearSorteo(userDetails.getIdUsuario(), peticion));
    }

    @Operation(summary = "Obtener detalle de un sorteo (público)")
    @GetMapping("/api/v1/sorteos/{idSorteo}")
    public ResponseEntity<RespuestaSorteo> obtenerSorteo(
            @PathVariable Long idSorteo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuarioActual = userDetails != null ? userDetails.getIdUsuario() : null;
        return ResponseEntity.ok(sorteoService.obtenerSorteo(idSorteo, idUsuarioActual));
    }

    @Operation(summary = "Editar un sorteo (CREADOR, con restricciones)")
    @PutMapping("/api/v1/sorteos/{idSorteo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaSorteo> actualizarSorteo(
            @PathVariable Long idSorteo,
            @Valid @RequestBody PeticionActualizarSorteo peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                sorteoService.actualizarSorteo(idSorteo, userDetails.getIdUsuario(), peticion));
    }

    @Operation(summary = "Eliminar un sorteo sin participantes (CREADOR)")
    @DeleteMapping("/api/v1/sorteos/{idSorteo}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> eliminarSorteo(
            @PathVariable Long idSorteo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(sorteoService.eliminarSorteo(idSorteo, userDetails.getIdUsuario()));
    }

    @Operation(summary = "Listar sorteos de un creador (público)")
    @GetMapping("/api/v1/creadores/{idPerfil}/sorteos")
    public ResponseEntity<List<RespuestaSorteo>> listarSorteosPorCreador(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuarioActual = userDetails != null ? userDetails.getIdUsuario() : null;
        return ResponseEntity.ok(sorteoService.listarSorteosPorCreador(idPerfil, idUsuarioActual));
    }

    @Operation(summary = "Listar sorteos activos (público)")
    @GetMapping("/api/v1/sorteos/activos")
    public ResponseEntity<List<RespuestaSorteo>> listarSorteosActivos(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuarioActual = userDetails != null ? userDetails.getIdUsuario() : null;
        return ResponseEntity.ok(sorteoService.listarSorteosActivos(idUsuarioActual));
    }

    // =========================================================================
    // Participación
    // =========================================================================

    @Operation(summary = "Inscribirse en un sorteo")
    @PostMapping("/api/v1/sorteos/{idSorteo}/participar")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RespuestaParticipante> participar(
            @PathVariable Long idSorteo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sorteoService.participar(idSorteo, userDetails.getIdUsuario()));
    }

    @Operation(summary = "Cancelar inscripción en un sorteo")
    @DeleteMapping("/api/v1/sorteos/{idSorteo}/participar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> cancelarParticipacion(
            @PathVariable Long idSorteo,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                sorteoService.cancelarParticipacion(idSorteo, userDetails.getIdUsuario()));
    }

    @Operation(summary = "Listar participantes de un sorteo")
    @GetMapping("/api/v1/sorteos/{idSorteo}/participantes")
    public ResponseEntity<List<RespuestaParticipante>> listarParticipantes(
            @PathVariable Long idSorteo) {
        return ResponseEntity.ok(sorteoService.listarParticipantes(idSorteo));
    }

    @Operation(summary = "Ver ganadores del sorteo (solo post-cierre)")
    @GetMapping("/api/v1/sorteos/{idSorteo}/ganadores")
    public ResponseEntity<List<RespuestaGanador>> listarGanadores(
            @PathVariable Long idSorteo) {
        return ResponseEntity.ok(sorteoService.listarGanadores(idSorteo));
    }
}
