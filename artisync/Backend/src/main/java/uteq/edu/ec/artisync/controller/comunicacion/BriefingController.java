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
import uteq.edu.ec.artisync.dto.peticion.comunicacion.CreateBriefingTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.BriefingResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.BriefingService;

import java.util.List;

/**
 * Controlador de briefing interactivo.
 * REQ-F-016 ampliado: el cuestionario se asigna a un servicio y el cliente lo
 * responde al crear el pedido (POST /api/v1/pedidos); este controlador solo
 * gestiona las plantillas del creador y la lectura de respuestas.
 */
@Tag(name = "Briefing", description = "Formulario interactivo de briefing para pedidos")
@RestController
@RequiredArgsConstructor
public class BriefingController {

    private final BriefingService briefingService;

    // =========================================================================
    // Gestión de plantillas (CREADOR)
    // =========================================================================

    /**
     * Registra una nueva plantilla de cuestionario de briefing para el creador autenticado.
     * @param peticion preguntas y configuración de la plantilla a crear
     * @param userDetails identidad del creador autenticado, usada para resolver su perfil
     * @return la plantilla de briefing recién creada
     */
    @Operation(summary = "Crear plantilla de briefing")
    @PostMapping("/api/v1/briefing/plantillas")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<BriefingResponse> crearPlantilla(
            @Valid @RequestBody CreateBriefingTemplateRequest peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        // BriefingServiceImpl resuelve el CreatorProfile propio a partir de este idUsuario.
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(briefingService.crearPlantilla(userDetails.getIdUsuario(), peticion));
    }

    /**
     * Obtiene todas las plantillas de briefing pertenecientes al creador autenticado.
     * @param userDetails identidad del creador autenticado, usada para filtrar sus plantillas
     * @return las plantillas de briefing del creador
     */
    @Operation(summary = "Listar mis plantillas de briefing")
    @GetMapping("/api/v1/briefing/plantillas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BriefingResponse>> obtenerMisPlantillas(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(briefingService.obtenerMisPlantillas(userDetails.getIdUsuario()));
    }

    /**
     * Actualiza las preguntas y configuración de una plantilla de briefing existente del creador.
     * @param idPlantilla identificador de la plantilla de briefing a editar
     * @param peticion nuevos datos de preguntas y configuración de la plantilla
     * @param userDetails identidad del creador autenticado, usada para validar la propiedad de la plantilla
     * @return la plantilla de briefing con los cambios aplicados
     */
    @Operation(summary = "Editar plantilla de briefing")
    @PutMapping("/api/v1/briefing/plantillas/{idPlantilla}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BriefingResponse> editarPlantilla(
            @PathVariable Long idPlantilla,
            @Valid @RequestBody CreateBriefingTemplateRequest peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(
                briefingService.editarPlantilla(idPlantilla, userDetails.getIdUsuario(), peticion));
    }

    /**
     * Elimina definitivamente una plantilla de briefing propiedad del creador autenticado.
     * @param idPlantilla identificador de la plantilla de briefing a eliminar
     * @param userDetails identidad del creador autenticado, usada para validar la propiedad de la plantilla
     * @return mensaje de confirmación de la eliminación
     */
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
    // Lectura del briefing respondido de un pedido
    // =========================================================================
    // El cliente ya no responde aquí: las respuestas se dan al crear el
    // pedido (POST /api/v1/pedidos, ver OrderServiceImpl.crearPedido).

    /**
     * Recupera las respuestas del cuestionario de briefing asociado a un pedido específico.
     * @param idPedido identificador del pedido cuyo briefing respondido se desea consultar
     * @param userDetails identidad del usuario autenticado, usada para validar que puede ver ese pedido
     * @return el briefing con las respuestas registradas al crear el pedido
     */
    @Operation(summary = "Ver el cuestionario respondido de un pedido")
    @GetMapping("/api/v1/pedidos/{idPedido}/briefing")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BriefingResponse> obtenerBriefing(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(briefingService.obtenerBriefing(idPedido, userDetails.getIdUsuario()));
    }
}
