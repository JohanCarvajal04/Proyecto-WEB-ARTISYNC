package uteq.edu.ec.artisync.service.comunicacion.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditContext;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.ViolationResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.entity.comunicacion.MessageViolation;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.comunicacion.ViolationRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.comunicacion.ViolationService;
import uteq.edu.ec.artisync.service.comunicacion.MessageFilterService;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;

import java.time.LocalDateTime;

/**
 * Implementación del servicio de infracciones.
 * RF-15: 3 infracciones en 30 días → suspensión automática de 15 días.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViolationServiceImpl implements ViolationService {

    private static final int PERIODO_DIAS      = 30;
    private static final int SUSPENSION_DIAS   = 15;

    private final ViolationRepository    infraccionRepo;
    private final UserRepository       usuarioRepo;
    private final MessageFilterService    mensajeFilterService;
    private final NotificationService     notificacionService;
    private final ObjectMapper            objectMapper;

    @Override
    // REQUIRES_NEW: quien llama a este metodo (ej. ChatServiceImpl.enviarMensaje)
    // suele lanzar una excepcion de regla de negocio inmediatamente despues,
    // dentro de su propia transaccion @Transactional -- con la propagacion
    // por defecto (REQUIRED) esa excepcion marca rollback-only y deshace este
    // INSERT y la suspension junto con todo lo demas de la transaccion del
    // llamador. Una transaccion propia hace que la infraccion quede
    // confirmada en el motor aunque el llamador aborte la suya despues.
    //
    // Jamás #mensaje en el detalle: el texto ya vive en
    // infracciones_mensaje.mensaje_original con su propio control de acceso;
    // duplicarlo en una bitácora que ni el ADMIN puede borrar empeoraría la
    // posición de privacidad. Solo se registra su longitud y el patrón.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditable(accion = "INFRACCION_REGISTRAR", modulo = AuditModule.COMUNICACION,
            entidad = "pedidos", idEntidad = "#idPedido")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param mensaje parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void registrarInfraccion(Long idUsuario, Long idPedido, String mensaje) {
        // REQ-F-015: fn_registrar_infraccion inserta la infraccion, cuenta el
        // total en la ventana de 30 dias y suspende la cuenta si corresponde,
        // todo en una unica transaccion atomica en el motor (evita la carrera
        // entre el COUNT y el UPDATE condicional que tenia la version en tres
        // llamadas independientes al repositorio).
        String patron = mensajeFilterService.detectarPatron(mensaje);
        AuditContext.aportar("idUsuarioInfractor", idUsuario);
        AuditContext.aportar("patronDetectado", patron);
        AuditContext.aportar("longitudMensaje", mensaje != null ? mensaje.length() : 0);

        String resultadoJson = infraccionRepo.registrarInfraccion(idUsuario, idPedido, mensaje, patron);
        JsonNode resultado = parseResultado(resultadoJson);
        int totalPeriodo = resultado.get("totalInfraccionesPeriodo").asInt();
        boolean cuentaSuspendida = resultado.get("cuentaSuspendida").asBoolean();

        log.info("Infracción registrada para usuario {}. Total en últimos {} días: {}",
                idUsuario, PERIODO_DIAS, totalPeriodo);

        if (cuentaSuspendida) {
            User usuario = usuarioRepo.findById(idUsuario)
                    .orElseThrow(() -> new ResourceNotFoundException("User no encontrado: " + idUsuario));
            notificarSuspension(usuario);
        }
    }

    private JsonNode parseResultado(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException("Error al interpretar el resultado de fn_registrar_infraccion", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param pageable configuracion de paginacion y ordenamiento para la capa de datos
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Page<ViolationResponse> listarInfracciones(Pageable pageable) {
        return infraccionRepo.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param pageable configuracion de paginacion y ordenamiento para la capa de datos
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Page<ViolationResponse> historialPorUsuario(Long idUsuario, Pageable pageable) {
        return infraccionRepo.findByUsuarioIdUsuario(idUsuario, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    @Auditable(accion = "SUSPENSION_REVERTIR", modulo = AuditModule.COMUNICACION,
            entidad = "usuarios", idEntidad = "#idUsuario")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaMensaje revertirSuspension(Long idUsuario) {
        User usuario = usuarioRepo.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado: " + idUsuario));
        usuario.setEstadoCuenta(true);
        usuarioRepo.save(usuario);
        log.info("Suspensión revertida para usuario {} por admin", idUsuario);
        return new RespuestaMensaje("Cuenta del usuario " + usuario.getCorreo() + " reactivada correctamente");
    }

    // -------------------------------------------------------------------------

    /** El estado_cuenta ya lo actualizo fn_registrar_infraccion; aqui solo se notifica. */
    private void notificarSuspension(User usuario) {
        LocalDateTime hastaFecha = LocalDateTime.now().plusDays(SUSPENSION_DIAS);
        String mensajeNotif = "Tu cuenta está suspendida hasta " + hastaFecha.toLocalDate()
                + " por superar el límite de infracciones de datos de contacto.";

        log.warn("Cuenta del usuario {} suspendida hasta {}", usuario.getCorreo(), hastaFecha.toLocalDate());
        notificacionService.notificar(usuario, "CUENTA_SUSPENDIDA", mensajeNotif);
    }

    private ViolationResponse mapToResponse(MessageViolation i) {
        return ViolationResponse.builder()
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
