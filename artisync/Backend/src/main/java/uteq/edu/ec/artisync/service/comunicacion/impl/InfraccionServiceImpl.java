package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaInfraccion;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.entity.comunicacion.InfraccionMensaje;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.comunicacion.InfraccionRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.InfraccionService;
import uteq.edu.ec.artisync.service.comunicacion.MensajeFilterService;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

import java.time.LocalDateTime;

/**
 * Implementación del servicio de infracciones.
 * RF-15: 3 infracciones en 30 días → suspensión automática de 15 días.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InfraccionServiceImpl implements InfraccionService {

    private static final int MAX_INFRACCIONES  = 3;
    private static final int PERIODO_DIAS      = 30;
    private static final int SUSPENSION_DIAS   = 15;

    private final InfraccionRepository    infraccionRepo;
    private final UsuarioRepository       usuarioRepo;
    private final MensajeFilterService    mensajeFilterService;
    private final NotificacionService     notificacionService;

    @Override
    @Transactional
    public void registrarInfraccion(Long idUsuario, Long idPedido, String mensaje) {
        Usuario usuario = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado: " + idUsuario));

        String patron = mensajeFilterService.detectarPatron(mensaje);

        InfraccionMensaje infraccion = InfraccionMensaje.builder()
                .usuario(usuario)
                // El pedido se deja null cuando se llama desde este servicio independiente;
                // ChatServiceImpl es el flujo primario que siempre incluye el pedido.
                .pedido(null)
                .mensajeOriginal(mensaje)
                .patronDetectado(patron)
                .build();
        infraccionRepo.save(infraccion);

        // Contar infracciones en los últimos PERIODO_DIAS días
        long count = infraccionRepo.countByUsuarioIdUsuarioAndFechaInfraccionAfter(
                idUsuario, LocalDateTime.now().minusDays(PERIODO_DIAS));

        log.info("Infracción registrada para usuario {}. Total en últimos {} días: {}",
                idUsuario, PERIODO_DIAS, count);

        if (count >= MAX_INFRACCIONES) {
            suspenderCuenta(usuario);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaInfraccion> listarInfracciones(Pageable pageable) {
        return infraccionRepo.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaInfraccion> historialPorUsuario(Long idUsuario, Pageable pageable) {
        return infraccionRepo.findAll(pageable)
                .map(this::mapToResponse)
                .map(r -> r.getIdUsuario().equals(idUsuario) ? r : null);
    }

    @Override
    @Transactional
    public RespuestaMensaje revertirSuspension(Long idUsuario) {
        Usuario usuario = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado: " + idUsuario));
        usuario.setEstadoCuenta(true);
        usuarioRepo.save(usuario);
        log.info("Suspensión revertida para usuario {} por admin", idUsuario);
        return new RespuestaMensaje("Cuenta del usuario " + usuario.getCorreo() + " reactivada correctamente");
    }

    // -------------------------------------------------------------------------

    private void suspenderCuenta(Usuario usuario) {
        usuario.setEstadoCuenta(false);
        usuarioRepo.save(usuario);

        LocalDateTime hastaFecha = LocalDateTime.now().plusDays(SUSPENSION_DIAS);
        String mensajeNotif = "Tu cuenta está suspendida hasta " + hastaFecha.toLocalDate()
                + " por superar el límite de infracciones de datos de contacto.";

        log.warn("Cuenta del usuario {} suspendida hasta {}", usuario.getCorreo(), hastaFecha.toLocalDate());
        notificacionService.notificar(usuario, "CUENTA_SUSPENDIDA", mensajeNotif);
    }

    private RespuestaInfraccion mapToResponse(InfraccionMensaje i) {
        return RespuestaInfraccion.builder()
                .idInfraccion(i.getIdInfraccion())
                .idUsuario(i.getUsuario().getIdUsuario())
                .nombreUsuario(i.getUsuario().getNombres() + " " + i.getUsuario().getApellidos())
                .correoUsuario(i.getUsuario().getCorreo())
                .idPedido(i.getPedido() != null ? i.getPedido().getIdPedido() : null)
                .mensajeOriginal(i.getMensajeOriginal())
                .patronDetectado(i.getPatronDetectado())
                .fechaInfraccion(i.getFechaInfraccion())
                .build();
    }
}
