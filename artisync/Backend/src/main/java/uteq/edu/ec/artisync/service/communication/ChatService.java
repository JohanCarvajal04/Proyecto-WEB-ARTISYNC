package uteq.edu.ec.artisync.service.communication;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.response.communication.ChatMessageResponse;
import uteq.edu.ec.artisync.dto.response.communication.ChatRoomResponse;
import uteq.edu.ec.artisync.entity.legal.ChatRoom;
import uteq.edu.ec.artisync.entity.order.Order;

/**
 * Offering de mensajería de chat vinculado a pedidos.
 * RF-14: Mensajería en tiempo real. RF-15: Filtrado de datos de contacto.
 */
public interface ChatService {

    /**
     * Crea la sala de chat al firmarse el contrato de un pedido.
     * @param pedido el pedido
     * @return el resultado de la operacion, de tipo {@code ChatRoom}
     */
    ChatRoom createRoom(Order pedido);

    /**
     * Cierra la sala y notifica vía WebSocket cuando el pedido finaliza.
     * @param idPedido el identificador de pedido
     */
    void closeRoom(Long idPedido);

    /**
     * Envía un mensaje aplicando el filtro RF-15.
     * Lanza BusinessRuleException si la sala está cerrada o el mensaje contiene datos de contacto.
     * @param idPedido el identificador de pedido
     * @param idRemitente el identificador de remitente
     * @param cuerpoMensaje el cuerpo mensaje
     * @return el resultado de la operacion, de tipo {@code ChatMessageResponse}
     */
    ChatMessageResponse sendMessage(Long idPedido, Long idRemitente, String cuerpoMensaje);

    /**
     * Historial de mensajes paginado de un pedido (por su sala).
     *
     * @param idUsuario quien consulta; debe ser el cliente o el creador del
     *                  pedido, o se rechaza (ver BusinessRuleException).
     * @param idPedido el identificador de pedido
     * @param pageable la pageable
     * @return la pagina de ChatMessageResponse encontrados
     */
    Page<ChatMessageResponse> getMessages(Long idPedido, Long idUsuario, Pageable pageable);

    /**
     * Estado actual de la sala (activa/cerrada).
     *
     * @param idUsuario quien consulta; debe ser el cliente o el creador del
     *                  pedido, o se rechaza (ver BusinessRuleException).
     * @param idPedido el identificador de pedido
     * @return el resultado de la operacion, de tipo {@code ChatRoomResponse}
     */
    ChatRoomResponse getRoomStatus(Long idPedido, Long idUsuario);
}
