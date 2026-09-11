package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaNotificacion;
import uteq.edu.ec.artisync.entity.comunicacion.NotificacionSistema;
import uteq.edu.ec.artisync.entity.comunicacion.TipoNotificacion;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.comunicacion.NotificacionSistemaRepository;
import uteq.edu.ec.artisync.repository.comunicacion.TipoNotificacionRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacionServiceImpl implements NotificacionService {

    private final NotificacionSistemaRepository notificacionRepo;
    private final TipoNotificacionRepository tipoNotificacionRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    /**
     * Despacha un mensaje o notificacion a los destinatarios especificados.
     *
     * @param destinatario parametro requerido para la correcta ejecucion del procedimiento
     * @param tipoEvento parametro requerido para la correcta ejecucion del procedimiento
     * @param mensajeTexto parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void notificar(Usuario destinatario, String tipoEvento, String mensajeTexto) {
        // notificar() casi siempre se llama desde dentro de la transacción de
        // una operación de negocio real (pago confirmado, ganador de sorteo,
        // etc.). Sin este try/catch, un fallo aquí (choque de UNIQUE al crear
        // TipoNotificacion por una carrera, o una excepción del broker STOMP
        // al hacer convertAndSendToUser) marca la transacción como
        // rollback-only y deshace en silencio el cambio de negocio ya
        // confirmado que la originó -- la notificación es un efecto
        // secundario de mejor esfuerzo, nunca debe poder tumbar eso.
        try {
            TipoNotificacion tipo = tipoNotificacionRepo.findByNombreEvento(tipoEvento)
                    .orElseGet(() -> {
                        TipoNotificacion nuevo = TipoNotificacion.builder()
                                .nombreEvento(tipoEvento)
                                .formatoMensaje(mensajeTexto)
                                .build();
                        return tipoNotificacionRepo.save(nuevo);
                    });

            NotificacionSistema notificacion = NotificacionSistema.builder()
                    .usuario(destinatario)
                    .tipoNotificacion(tipo)
                    .mensaje(mensajeTexto)
                    .estaLeida(false)
                    .build();
            notificacion = notificacionRepo.save(notificacion);

            // Entrega en tiempo real al usuario via WebSocket (canal privado)
            RespuestaNotificacion dto = mapToResponse(notificacion, mensajeTexto);
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Page<RespuestaNotificacion> listarMisNotificaciones(Long idUsuario, Pageable pageable) {
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaNotificacion marcarComoLeida(Long idNotificacion, Long idUsuario) {
        NotificacionSistema notificacion = notificacionRepo.findById(idNotificacion)
                .filter(n -> n.getUsuario().getIdUsuario().equals(idUsuario))
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public long contarNoLeidas(Long idUsuario) {
        return notificacionRepo.countByUsuarioIdUsuarioAndEstaLeidaFalse(idUsuario);
    }

    // -------------------------------------------------------------------------
    private RespuestaNotificacion mapToResponse(NotificacionSistema n, String mensajeTexto) {
        return RespuestaNotificacion.builder()
                .idNotificacion(n.getIdNotificacion())
                .tipoEvento(n.getTipoNotificacion().getNombreEvento())
                .mensaje(mensajeTexto)
                .estaLeida(n.getEstaLeida())
                .fechaEmision(n.getFechaEmision())
                .build();
    }
}
