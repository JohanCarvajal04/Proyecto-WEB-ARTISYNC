package uteq.edu.ec.artisync.service.comunicacion.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ContextoAuditoria;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
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

    private static final int PERIODO_DIAS      = 30;
    private static final int SUSPENSION_DIAS   = 15;

    private final InfraccionRepository    infraccionRepo;
    private final UsuarioRepository       usuarioRepo;
    private final MensajeFilterService    mensajeFilterService;
    private final NotificacionService     notificacionService;
    private final ObjectMapper            objectMapper;

    @Override
    @Transactional
    // Jamás #mensaje en el detalle: el texto ya vive en
    // infracciones_mensaje.mensaje_original con su propio control de acceso;
    // duplicarlo en una bitácora que ni el ADMIN puede borrar empeoraría la
    // posición de privacidad. Solo se registra su longitud y el patrón.
    @Auditable(accion = "INFRACCION_REGISTRAR", modulo = ModuloAuditoria.COMUNICACION,
            entidad = "pedidos", idEntidad = "#idPedido")
    public void registrarInfraccion(Long idUsuario, Long idPedido, String mensaje) {
        // REQ-F-015: fn_registrar_infraccion inserta la infraccion, cuenta el
        // total en la ventana de 30 dias y suspende la cuenta si corresponde,
        // todo en una unica transaccion atomica en el motor (evita la carrera
        // entre el COUNT y el UPDATE condicional que tenia la version en tres
        // llamadas independientes al repositorio).
        String patron = mensajeFilterService.detectarPatron(mensaje);
        ContextoAuditoria.aportar("idUsuarioInfractor", idUsuario);
        ContextoAuditoria.aportar("patronDetectado", patron);
        ContextoAuditoria.aportar("longitudMensaje", mensaje != null ? mensaje.length() : 0);

        String resultadoJson = infraccionRepo.registrarInfraccion(idUsuario, idPedido, mensaje, patron);
        JsonNode resultado = parseResultado(resultadoJson);
        int totalPeriodo = resultado.get("totalInfraccionesPeriodo").asInt();
        boolean cuentaSuspendida = resultado.get("cuentaSuspendida").asBoolean();

        log.info("Infracción registrada para usuario {}. Total en últimos {} días: {}",
                idUsuario, PERIODO_DIAS, totalPeriodo);

        if (cuentaSuspendida) {
            Usuario usuario = usuarioRepo.findById(idUsuario)
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado: " + idUsuario));
            notificarSuspension(usuario);
        }
    }

    private JsonNode parseResultado(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException("Error al interpretar el resultado de fn_registrar_infraccion", e);
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
    @Auditable(accion = "SUSPENSION_REVERTIR", modulo = ModuloAuditoria.COMUNICACION,
            entidad = "usuarios", idEntidad = "#idUsuario")
    public RespuestaMensaje revertirSuspension(Long idUsuario) {
        Usuario usuario = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado: " + idUsuario));
        usuario.setEstadoCuenta(true);
        usuarioRepo.save(usuario);
        log.info("Suspensión revertida para usuario {} por admin", idUsuario);
        return new RespuestaMensaje("Cuenta del usuario " + usuario.getCorreo() + " reactivada correctamente");
    }

    // -------------------------------------------------------------------------

    /** El estado_cuenta ya lo actualizo fn_registrar_infraccion; aqui solo se notifica. */
    private void notificarSuspension(Usuario usuario) {
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
