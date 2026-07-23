package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaNotificacion;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

import java.util.Map;

/**
 * Centro de notificaciones del sistema.
 * Transversal: recibe notificaciones de todos los módulos.
 */
@Tag(name = "Notificaciones", description = "Centro de notificaciones del sistema")
@RestController
@RequestMapping("/api/v1/notificaciones")
@RequiredArgsConstructor
public class NotificacionControlador {

    private final NotificacionService notificacionService;

    @Operation(summary = "Listar mis notificaciones (paginado)")
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<RespuestaNotificacion>> listarMisNotificaciones(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable) {
        return ResponseEntity.ok(
                notificacionService.listarMisNotificaciones(userDetails.getIdUsuario(), pageable));
    }

    @Operation(summary = "Marcar una notificación como leída")
    @PutMapping("/{id}/leer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaNotificacion> marcarComoLeida(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                notificacionService.marcarComoLeida(id, userDetails.getIdUsuario()));
    }

    @Operation(summary = "Marcar todas las notificaciones como leídas")
    @PutMapping("/leer-todas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> marcarTodasLeidas(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        int count = notificacionService.marcarTodasLeidas(userDetails.getIdUsuario());
        return ResponseEntity.ok(new RespuestaMensaje(count + " notificaciones marcadas como leídas"));
    }

    @Operation(summary = "Contador de notificaciones no leídas")
    @GetMapping("/no-leidas/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> contarNoLeidas(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = notificacionService.contarNoLeidas(userDetails.getIdUsuario());
        return ResponseEntity.ok(Map.of("noLeidas", count));
    }
}
