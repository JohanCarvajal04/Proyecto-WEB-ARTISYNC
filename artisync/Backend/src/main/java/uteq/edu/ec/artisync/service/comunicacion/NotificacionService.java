package uteq.edu.ec.artisync.service.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaNotificacion;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;

/**
 * Servicio transversal de notificaciones.
 * Utilizado por todos los módulos para enviar alertas en tiempo real al usuario.
 */
public interface NotificacionService {

    /**
     * Persiste una notificación y la entrega en tiempo real vía WebSocket.
     *
     * @param destinatario usuario que recibirá la notificación
     * @param tipoEvento   clave del tipo de evento (ej. "CUENTA_SUSPENDIDA")
     * @param mensajeTexto texto visible para el usuario
     */
    void notificar(Usuario destinatario, String tipoEvento, String mensajeTexto);

    Page<RespuestaNotificacion> listarMisNotificaciones(Long idUsuario, Pageable pageable);

    RespuestaNotificacion marcarComoLeida(Long idNotificacion, Long idUsuario);

    int marcarTodasLeidas(Long idUsuario);

    long contarNoLeidas(Long idUsuario);
}
