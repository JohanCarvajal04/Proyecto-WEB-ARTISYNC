package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaCreadorSeguidoNovedad;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoSeguimiento;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.ISeguidorServicio;

import java.util.List;

@Tag(name = "Seguidores", description = "Gestión de seguimiento a perfiles de creadores")
@RestController
@RequestMapping("/api/v1/creadores")
@RequiredArgsConstructor
public class SeguidorControlador {

    private final ISeguidorServicio seguidorServicio;

    @Operation(summary = "Seguir a un perfil de creador")
    @PostMapping("/{idPerfil}/seguir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaEstadoSeguimiento> seguirCreador(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(seguidorServicio.seguirCreador(userDetails.getIdUsuario(), idPerfil));
    }

    @Operation(summary = "Dejar de seguir a un perfil de creador")
    @DeleteMapping("/{idPerfil}/seguir")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaEstadoSeguimiento> dejarDeSeguirCreador(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(seguidorServicio.dejarDeSeguirCreador(userDetails.getIdUsuario(), idPerfil));
    }

    @Operation(summary = "Obtener el estado de seguimiento y conteo de un perfil de creador")
    @GetMapping("/{idPerfil}/es-seguidor")
    public ResponseEntity<RespuestaEstadoSeguimiento> obtenerEstadoSeguimiento(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long idUsuario = userDetails != null ? userDetails.getIdUsuario() : null;
        return ResponseEntity.ok(seguidorServicio.obtenerEstadoSeguimiento(idUsuario, idPerfil));
    }

    @Operation(summary = "Listar los seguidores de un perfil de creador")
    @GetMapping("/{idPerfil}/seguidores")
    public ResponseEntity<List<RespuestaSeguidor>> listarSeguidores(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(seguidorServicio.listarSeguidores(idPerfil));
    }

    @Operation(summary = "Listar las novedades de los creadores que el usuario sigue")
    @GetMapping("/siguiendo/novedades")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RespuestaCreadorSeguidoNovedad>> listarCreadoresSeguidosNovedades(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(seguidorServicio.listarCreadoresSeguidosNovedades(userDetails.getIdUsuario()));
    }

    @Operation(summary = "Actualizar la portada y título profesional del creador")
    @PutMapping("/mi-perfil/portada")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> actualizarPortadaYTitulo(
            @RequestParam(required = false) String urlPortada,
            @RequestParam(required = false) String tituloProfesional,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        seguidorServicio.actualizarPortadaYTitulo(userDetails.getIdUsuario(), urlPortada, tituloProfesional);
        return ResponseEntity.ok(new RespuestaMensaje("Portada y especialidad del perfil actualizadas correctamente."));
    }
}
