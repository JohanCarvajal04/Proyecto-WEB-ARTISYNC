package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearBriefingPlantilla;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarBriefing;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionResponderBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.BriefingService;

import java.util.List;

/**
 * Controlador de briefing interactivo.
 * RF-16: Formulario configurable de hasta 10 preguntas; respuestas inmutables.
 */
@Tag(name = "Briefing", description = "Formulario interactivo de briefing para pedidos")
@RestController
@RequiredArgsConstructor
public class BriefingControlador {

    private final BriefingService briefingService;

    // =========================================================================
    // Gestión de plantillas (CREADOR)
    // =========================================================================

    @Operation(summary = "Crear plantilla de briefing")
    @PostMapping("/api/v1/briefing/plantillas")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RespuestaBriefing> crearPlantilla(
            @Valid @RequestBody PeticionCrearBriefingPlantilla peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // El idPerfilCreador se resuelve desde el JWT del usuario autenticado
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(briefingService.crearPlantilla(userDetails.getIdUsuario(), peticion));
    }

    @Operation(summary = "Listar mis plantillas de briefing")
    @GetMapping("/api/v1/briefing/plantillas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RespuestaBriefing>> obtenerMisPlantillas(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(briefingService.obtenerMisPlantillas(userDetails.getIdUsuario()));
    }

    @Operation(summary = "Editar plantilla de briefing")
    @PutMapping("/api/v1/briefing/plantillas/{idPlantilla}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaBriefing> editarPlantilla(
            @PathVariable Long idPlantilla,
            @Valid @RequestBody PeticionCrearBriefingPlantilla peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                briefingService.editarPlantilla(idPlantilla, userDetails.getIdUsuario(), peticion));
    }

    @Operation(summary = "Eliminar plantilla de briefing")
    @DeleteMapping("/api/v1/briefing/plantillas/{idPlantilla}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensaje> eliminarPlantilla(
            @PathVariable Long idPlantilla,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                briefingService.eliminarPlantilla(idPlantilla, userDetails.getIdUsuario()));
    }

    // =========================================================================
    // Envío y respuesta de briefing en pedidos
    // =========================================================================

    @Operation(summary = "Enviar briefing al cliente de un pedido (CREADOR)")
    @PostMapping("/api/v1/pedidos/{idPedido}/briefing")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RespuestaBriefing> enviarBriefing(
            @PathVariable Long idPedido,
            @Valid @RequestBody PeticionEnviarBriefing peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(briefingService.enviarBriefing(idPedido, peticion, userDetails.getIdUsuario()));
    }

    @Operation(summary = "Ver briefing enviado a un pedido")
    @GetMapping("/api/v1/pedidos/{idPedido}/briefing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaBriefing> obtenerBriefing(@PathVariable Long idPedido) {
        return ResponseEntity.ok(briefingService.obtenerBriefing(idPedido));
    }

    @Operation(summary = "Responder briefing (CLIENTE) — respuestas inmutables")
    @PostMapping("/api/v1/pedidos/{idPedido}/briefing/responder")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaBriefing> responderBriefing(
            @PathVariable Long idPedido,
            @Valid @RequestBody PeticionResponderBriefing peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                briefingService.responderBriefing(idPedido, peticion, userDetails.getIdUsuario()));
    }
}
