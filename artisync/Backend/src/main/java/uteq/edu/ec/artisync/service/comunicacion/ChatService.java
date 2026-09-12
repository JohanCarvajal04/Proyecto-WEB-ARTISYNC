package uteq.edu.ec.artisync.service.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.ChatMessageResponse;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.ChatRoomResponse;
import uteq.edu.ec.artisync.entity.legal.ChatRoom;
import uteq.edu.ec.artisync.entity.pedido.Order;

/**
 * Offering de mensajería de chat vinculado a pedidos.
 * RF-14: Mensajería en tiempo real. RF-15: Filtrado de datos de contacto.
 */
public interface ChatService {

    /** Crea la sala de chat al firmarse el contrato de un pedido. */
    ChatRoom createRoom(Order pedido);

    /** Cierra la sala y notifica vía WebSocket cuando el pedido finaliza. */
    void closeRoom(Long idPedido);

    /**
     * Envía un mensaje aplicando el filtro RF-15.
     * Lanza BusinessRuleException si la sala está cerrada o el mensaje contiene datos de contacto.
     */
    ChatMessageResponse sendMessage(Long idPedido, Long idRemitente, String cuerpoMensaje);

    /**
     * Historial de mensajes paginado de un pedido (por su sala).
     *
     * @param idUsuario quien consulta; debe ser el cliente o el creador del
     *                  pedido, o se rechaza (ver BusinessRuleException).
     */
    Page<ChatMessageResponse> getMessages(Long idPedido, Long idUsuario, Pageable pageable);

    /**
     * Estado actual de la sala (activa/cerrada).
     *
     * @param idUsuario quien consulta; debe ser el cliente o el creador del
     *                  pedido, o se rechaza (ver BusinessRuleException).
     */
    ChatRoomResponse getRoomStatus(Long idPedido, Long idUsuario);
}
