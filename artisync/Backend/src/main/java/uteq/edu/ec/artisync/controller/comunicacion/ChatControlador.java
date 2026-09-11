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
import uteq.edu.ec.artisync.exception.BusinessRuleException;
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

    /**
     * Obtiene el historial paginado de mensajes de la sala de chat de un pedido.
     *
     * @param idPedido identificador del pedido
     * @param pageable configuración de paginación
     * @param userDetails usuario autenticado que solicita el historial
     * @return página con los mensajes de la sala de chat
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si no existe sala de chat para el pedido
     * @throws BusinessRuleException si el usuario no participa en el pedido
     */
    @Operation(summary = "Historial de mensajes de un pedido (paginado)")
    @GetMapping("/mensajes")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<RespuestaMensajeChat>> obtenerMensajes(
            @PathVariable Long idPedido,
            Pageable pageable,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatService.obtenerMensajes(idPedido, userDetails.getIdUsuario(), pageable));
    }

    /**
     * Envía un mensaje al chat de un pedido por REST, como alternativa al canal WebSocket.
     *
     * @param idPedido identificador del pedido
     * @param peticion cuerpo del mensaje a enviar
     * @param userDetails usuario autenticado que envía el mensaje
     * @return el mensaje enviado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si no existe sala de chat para el pedido
     * @throws BusinessRuleException si el usuario no participa en el pedido, la sala está cerrada o el mensaje contiene datos de contacto
     */
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

    /**
     * Obtiene el estado actual de la sala de chat de un pedido.
     *
     * @param idPedido identificador del pedido
     * @param userDetails usuario autenticado que consulta el estado
     * @return el estado de la sala de chat
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si no existe sala de chat para el pedido
     * @throws BusinessRuleException si el usuario no participa en el pedido
     */
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
     *
     * @param peticion mensaje entrante con el identificador del pedido y el cuerpo del mensaje
     * @param userDetails usuario autenticado que envía el mensaje
     * @throws BusinessRuleException si no se indica el identificador del pedido, el usuario no participa en él,
     *      la sala está cerrada o el mensaje contiene datos de contacto
     */
    @MessageMapping("/chat.enviar")
    public void enviarMensajeWs(
            @Payload PeticionEnviarMensaje peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (peticion.getIdPedido() == null) {
            log.warn("Message STOMP recibido sin idPedido, usuario {}", userDetails.getIdUsuario());
            throw new BusinessRuleException("idPedido es obligatorio para enviar mensajes por WebSocket");
        }
        chatService.enviarMensaje(peticion.getIdPedido(), userDetails.getIdUsuario(), peticion.getCuerpoMensaje());
    }
}
