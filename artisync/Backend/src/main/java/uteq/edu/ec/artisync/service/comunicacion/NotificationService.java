package uteq.edu.ec.artisync.service.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.NotificationResponse;
import uteq.edu.ec.artisync.entity.seguridad.User;

/**
 * Offering transversal de notificaciones.
 * Utilizado por todos los módulos para enviar alertas en tiempo real al usuario.
 */
public interface NotificationService {

    /**
     * Persiste una notificación y la entrega en tiempo real vía WebSocket.
     *
     * @param destinatario usuario que recibirá la notificación
     * @param tipoEvento   clave del tipo de evento (ej. "CUENTA_SUSPENDIDA")
     * @param mensajeTexto texto visible para el usuario
     */
    void notificar(User destinatario, String tipoEvento, String mensajeTexto);

    /**
     * Lista las notificaciones del usuario, más recientes primero.
     *
     * @param idUsuario identificador del usuario destinatario
     * @param pageable configuración de paginación y ordenamiento
     * @return la página de notificaciones del usuario
     */
    Page<NotificationResponse> listarMisNotificaciones(Long idUsuario, Pageable pageable);

    /**
     * Marca una notificación como leída, si pertenece al usuario indicado.
     *
     * @param idNotificacion identificador de la notificación
     * @param idUsuario identificador del usuario que la marca como leída
     * @return la notificación ya marcada como leída
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la notificación no existe o no pertenece al usuario
     */
    NotificationResponse marcarComoLeida(Long idNotificacion, Long idUsuario);

    /**
     * Marca como leídas todas las notificaciones pendientes del usuario.
     *
     * @param idUsuario identificador del usuario
     * @return la cantidad de notificaciones marcadas como leídas
     */
    int marcarTodasLeidas(Long idUsuario);

    /**
     * Cuenta las notificaciones no leídas del usuario.
     *
     * @param idUsuario identificador del usuario
     * @return la cantidad de notificaciones no leídas
     */
    long contarNoLeidas(Long idUsuario);
}
