package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.SeguidorService;

import java.util.List;
import java.util.Map;

/**
 * Controlador de seguimiento de creadores (REQ-F-009).
 *
 * Las lecturas públicas del perfil (contador y listado de seguidores) cuelgan de
 * {@code /api/v1/creadores/**}, que {@code SecurityConfig} declara {@code permitAll}.
 * Las escrituras cuelgan de {@code /api/v1/social/**}, que cae en
 * {@code anyRequest().authenticated()}: así la autenticación la exige la cadena de
 * filtros y no solo {@code @PreAuthorize}, siguiendo el mismo reparto que ya usan
 * las lecturas de {@code /api/v1/creadores} frente a las escrituras de
 * {@code /api/v1/perfiles}.
 */
@Tag(name = "Seguidores", description = "Seguir y dejar de seguir creadores")
@RestController
@RequiredArgsConstructor
public class SeguidorControlador {

    private final SeguidorService seguidorService;

    @Operation(summary = "Seguir a un creador (usuario autenticado)")
    @PostMapping("/api/v1/social/creadores/{idPerfil}/seguidores")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaSeguidor> seguir(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seguidorService.seguir(idPerfil, userDetails.getIdUsuario()));
    }

    @Operation(summary = "Dejar de seguir a un creador (usuario autenticado)")
    @DeleteMapping("/api/v1/social/creadores/{idPerfil}/seguidores")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> dejarDeSeguir(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        seguidorService.dejarDeSeguir(idPerfil, userDetails.getIdUsuario());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Estado de seguimiento del usuario actual sobre un creador")
    @GetMapping("/api/v1/social/creadores/{idPerfil}/seguidores/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> estado(
            @PathVariable Long idPerfil,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
                "idPerfil", idPerfil,
                "siguiendo", seguidorService.sigue(idPerfil, userDetails.getIdUsuario()),
                "totalSeguidores", seguidorService.contarSeguidores(idPerfil)
        ));
    }

    @Operation(summary = "Contador de seguidores de un creador (público)")
    @GetMapping("/api/v1/creadores/{idPerfil}/seguidores/contador")
    public ResponseEntity<Map<String, Object>> contar(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(Map.of(
                "idPerfil", idPerfil,
                "totalSeguidores", seguidorService.contarSeguidores(idPerfil)
        ));
    }

    @Operation(summary = "Listar seguidores de un creador (público)")
    @GetMapping("/api/v1/creadores/{idPerfil}/seguidores")
    public ResponseEntity<List<RespuestaSeguidor>> listar(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(seguidorService.listarSeguidores(idPerfil));
    }
}
