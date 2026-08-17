package uteq.edu.ec.artisync.controller.social;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoSeguimiento;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.SeguidorService;

import java.util.List;

/**
 * Controlador para la gestión de seguidores.
 * REQ-F-009: El perfil público muestra seguidores; cualquier usuario autenticado puede seguir/dejar de seguir.
 */
@Tag(name = "Seguidores", description = "Gestión de seguidores y comunidad")
@RestController
@RequiredArgsConstructor
public class SeguidorControlador {

    private final SeguidorService seguidorService;

    @Operation(summary = "Seguir a un creador")
    @PostMapping("/api/v1/creadores/{idPerfil}/seguir")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RespuestaSeguidor> seguirCreador(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seguidorService.seguirCreador(userDetails.getIdUsuario(), idPerfil));
    }

    @Operation(summary = "Dejar de seguir a un creador")
    @DeleteMapping("/api/v1/creadores/{idPerfil}/seguir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> dejarDeSeguir(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(seguidorService.dejarDeSeguir(userDetails.getIdUsuario(), idPerfil));
    }

    @Operation(summary = "Obtener estado de seguimiento y conteo total de seguidores")
    @GetMapping("/api/v1/creadores/{idPerfil}/seguir/estado")
    public ResponseEntity<RespuestaEstadoSeguimiento> obtenerEstadoSeguimiento(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long idUsuarioActual = userDetails != null ? userDetails.getIdUsuario() : null;
        boolean isFollowing = seguidorService.estaSiguiendo(idUsuarioActual, idPerfil);
        long count = seguidorService.contarSeguidores(idPerfil);
        
        return ResponseEntity.ok(new RespuestaEstadoSeguimiento(isFollowing, count));
    }

    @Operation(summary = "Listar los seguidores de un creador")
    @GetMapping("/api/v1/creadores/{idPerfil}/seguidores")
    public ResponseEntity<List<RespuestaSeguidor>> listarSeguidores(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(seguidorService.listarSeguidores(idPerfil));
    }
}
