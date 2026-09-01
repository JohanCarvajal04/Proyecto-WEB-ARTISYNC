package uteq.edu.ec.artisync.service.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaMensajeChat;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSalaChat;
import uteq.edu.ec.artisync.entity.legal.SalaChat;
import uteq.edu.ec.artisync.entity.pedido.Pedido;

/**
 * Servicio de mensajería de chat vinculado a pedidos.
 * RF-14: Mensajería en tiempo real. RF-15: Filtrado de datos de contacto.
 */
public interface ChatService {

    /** Crea la sala de chat al firmarse el contrato de un pedido. */
    SalaChat crearSala(Pedido pedido);

    /** Cierra la sala y notifica vía WebSocket cuando el pedido finaliza. */
    void cerrarSala(Long idPedido);

    /**
     * Envía un mensaje aplicando el filtro RF-15.
     * Lanza ExcepcionReglaNegocio si la sala está cerrada o el mensaje contiene datos de contacto.
     */
    RespuestaMensajeChat enviarMensaje(Long idPedido, Long idRemitente, String cuerpoMensaje);

    /**
     * Historial de mensajes paginado de un pedido (por su sala).
     *
     * @param idUsuario quien consulta; debe ser el cliente o el creador del
     *                  pedido, o se rechaza (ver ExcepcionReglaNegocio).
     */
    Page<RespuestaMensajeChat> obtenerMensajes(Long idPedido, Long idUsuario, Pageable pageable);

    /**
     * Estado actual de la sala (activa/cerrada).
     *
     * @param idUsuario quien consulta; debe ser el cliente o el creador del
     *                  pedido, o se rechaza (ver ExcepcionReglaNegocio).
     */
    RespuestaSalaChat obtenerEstadoSala(Long idPedido, Long idUsuario);
}
