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
    public void notificar(Usuario destinatario, String tipoEvento, String mensajeTexto) {
        // Busca o crea el tipo de notificación dinámicamente
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
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaNotificacion> listarMisNotificaciones(Long idUsuario, Pageable pageable) {
        return notificacionRepo
                .findByUsuarioIdUsuarioOrderByFechaEmisionDesc(idUsuario, pageable)
                .map(n -> mapToResponse(n, n.getTipoNotificacion().getFormatoMensaje()));
    }

    @Override
    @Transactional
    public RespuestaNotificacion marcarComoLeida(Long idNotificacion, Long idUsuario) {
        NotificacionSistema notificacion = notificacionRepo.findById(idNotificacion)
                .filter(n -> n.getUsuario().getIdUsuario().equals(idUsuario))
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Notificación no encontrada o no pertenece al usuario"));
        notificacion.setEstaLeida(true);
        notificacion = notificacionRepo.save(notificacion);
        return mapToResponse(notificacion, notificacion.getTipoNotificacion().getFormatoMensaje());
    }

    @Override
    @Transactional
    public int marcarTodasLeidas(Long idUsuario) {
        return notificacionRepo.marcarTodasLeidas(idUsuario);
    }

    @Override
    @Transactional(readOnly = true)
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
