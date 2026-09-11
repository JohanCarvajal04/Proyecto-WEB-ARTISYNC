package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.NotificationResponse;
import uteq.edu.ec.artisync.entity.comunicacion.SystemNotification;
import uteq.edu.ec.artisync.entity.comunicacion.NotificationType;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.comunicacion.SystemNotificationRepository;
import uteq.edu.ec.artisync.repository.comunicacion.NotificationTypeRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SystemNotificationRepository notificacionRepo;
    private final NotificationTypeRepository tipoNotificacionRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    /**
     * Despacha un mensaje o notificacion a los destinatarios especificados.
     *
     * @param destinatario parametro requerido para la correcta ejecucion del procedimiento
     * @param tipoEvento parametro requerido para la correcta ejecucion del procedimiento
     * @param mensajeTexto parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void notificar(User destinatario, String tipoEvento, String mensajeTexto) {
        // notificar() casi siempre se llama desde dentro de la transacción de
        // una operación de negocio real (pago confirmado, ganador de sorteo,
        // etc.). Sin este try/catch, un fallo aquí (choque de UNIQUE al crear
        // NotificationType por una carrera, o una excepción del broker STOMP
        // al hacer convertAndSendToUser) marca la transacción como
        // rollback-only y deshace en silencio el cambio de negocio ya
        // confirmado que la originó -- la notificación es un efecto
        // secundario de mejor esfuerzo, nunca debe poder tumbar eso.
        try {
            NotificationType tipo = tipoNotificacionRepo.findByNombreEvento(tipoEvento)
                    .orElseGet(() -> {
                        NotificationType nuevo = NotificationType.builder()
                                .nombreEvento(tipoEvento)
                                .formatoMensaje(mensajeTexto)
                                .build();
                        return tipoNotificacionRepo.save(nuevo);
                    });

            SystemNotification notificacion = SystemNotification.builder()
                    .usuario(destinatario)
                    .tipoNotificacion(tipo)
                    .mensaje(mensajeTexto)
                    .estaLeida(false)
                    .build();
            notificacion = notificacionRepo.save(notificacion);

            // Entrega en tiempo real al usuario via WebSocket (canal privado)
            NotificationResponse dto = mapToResponse(notificacion, mensajeTexto);
            messagingTemplate.convertAndSendToUser(
                    destinatario.getCorreo(),
                    "/queue/notificaciones",
                    dto
            );
            log.debug("Notificación '{}' enviada a {}", tipoEvento, destinatario.getCorreo());
        } catch (Exception e) {
            log.error("No se pudo entregar la notificación '{}' a {}: {}",
                    tipoEvento, destinatario.getCorreo(), e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param pageable configuracion de paginacion y ordenamiento para la capa de datos
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Page<NotificationResponse> listarMisNotificaciones(Long idUsuario, Pageable pageable) {
        return notificacionRepo
                .findByUsuarioIdUsuarioOrderByFechaEmisionDesc(idUsuario, pageable)
                .map(n -> mapToResponse(n, n.getMensaje()));
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idNotificacion identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public NotificationResponse marcarComoLeida(Long idNotificacion, Long idUsuario) {
        SystemNotification notificacion = notificacionRepo.findById(idNotificacion)
                .filter(n -> n.getUsuario().getIdUsuario().equals(idUsuario))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notificación no encontrada o no pertenece al usuario"));
        notificacion.setEstaLeida(true);
        notificacion = notificacionRepo.save(notificacion);
        return mapToResponse(notificacion, notificacion.getMensaje());
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public int marcarTodasLeidas(Long idUsuario) {
        return notificacionRepo.marcarTodasLeidas(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public long contarNoLeidas(Long idUsuario) {
        return notificacionRepo.countByUsuarioIdUsuarioAndEstaLeidaFalse(idUsuario);
    }

    // -------------------------------------------------------------------------
    private NotificationResponse mapToResponse(SystemNotification n, String mensajeTexto) {
        return NotificationResponse.builder()
                .idNotificacion(n.getIdNotificacion())
                .tipoEvento(n.getTipoNotificacion().getNombreEvento())
                .mensaje(mensajeTexto)
                .estaLeida(n.getEstaLeida())
                .fechaEmision(n.getFechaEmision())
                .build();
    }
}
