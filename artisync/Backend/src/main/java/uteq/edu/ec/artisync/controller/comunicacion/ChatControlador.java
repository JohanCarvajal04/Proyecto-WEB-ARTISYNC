package uteq.edu.ec.artisync.controller.comunicacion;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaMensajeChat;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSalaChat;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;

/**
 * Controlador de chat — REST + WebSocket STOMP.
 * RF-14: Mensajería en tiempo real. RF-15: Filtrado de datos de contacto.
 */
@Slf4j
@Tag(name = "Chat", description = "Mensajería interna en tiempo real vinculada a pedidos")
@RestController
@RequestMapping("/api/v1/pedidos/{idPedido}/chat")
@RequiredArgsConstructor
public class ChatControlador {

    private final ChatService chatService;

    @Operation(summary = "Historial de mensajes de un pedido (paginado)")
    @GetMapping("/mensajes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<RespuestaMensajeChat>> obtenerMensajes(
            @PathVariable Long idPedido,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatService.obtenerMensajes(idPedido, userDetails.getIdUsuario(), pageable));
    }

    @Operation(summary = "Enviar mensaje por REST (fallback sin WebSocket)")
    @PostMapping("/mensajes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaMensajeChat> enviarMensaje(
            @PathVariable Long idPedido,
            @Valid @RequestBody PeticionEnviarMensaje peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        RespuestaMensajeChat respuesta = chatService.enviarMensaje(
                idPedido, userDetails.getIdUsuario(), peticion.getCuerpoMensaje());
        return ResponseEntity.ok(respuesta);
    }

    @Operation(summary = "Estado actual de la sala de chat de un pedido")
    @GetMapping("/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<RespuestaSalaChat> obtenerEstado(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatService.obtenerEstadoSala(idPedido, userDetails.getIdUsuario()));
    }

    // -------------------------------------------------------------------------
    // WebSocket STOMP — manejo de mensajes entrantes
    // -------------------------------------------------------------------------

    /**
     * Endpoint STOMP: el cliente envía a /app/chat.enviar con el idPedido y cuerpo del mensaje.
     * La respuesta se publica automáticamente en /topic/sala.{idSala} desde ChatServiceImpl.
     */
    @MessageMapping("/chat.enviar")
    public void enviarMensajeWs(
            @Payload PeticionEnviarMensaje peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (peticion.getIdPedido() == null) {
            log.warn("Mensaje STOMP recibido sin idPedido, usuario {}", userDetails.getIdUsuario());
            throw new ExcepcionReglaNegocio("idPedido es obligatorio para enviar mensajes por WebSocket");
        }
        chatService.enviarMensaje(peticion.getIdPedido(), userDetails.getIdUsuario(), peticion.getCuerpoMensaje());
    }
}
