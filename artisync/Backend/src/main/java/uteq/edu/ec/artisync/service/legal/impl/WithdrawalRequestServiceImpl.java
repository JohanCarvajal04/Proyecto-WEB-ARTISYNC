package uteq.edu.ec.artisync.service.legal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.legal.WithdrawalRequestFilter;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateWithdrawalRequest;
import uteq.edu.ec.artisync.dto.respuesta.legal.CreatorBalanceResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.WithdrawalRequestResponse;
import uteq.edu.ec.artisync.entity.legal.WithdrawalRequest;
import uteq.edu.ec.artisync.entity.perfil.CreatorPaymentDetails;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.WithdrawalRequestRepository;
import uteq.edu.ec.artisync.repository.legal.PaymentTransactionRepository;
import uteq.edu.ec.artisync.repository.perfil.CreatorPaymentDetailsRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.legal.IWithdrawalRequestService;
import uteq.edu.ec.artisync.service.shared.paypal.PayPalClient;
import uteq.edu.ec.artisync.specification.legal.WithdrawalRequestSpecification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WithdrawalRequestServiceImpl implements IWithdrawalRequestService {

    private static final String ESTADO_PENDIENTE = "Pendiente";
    private static final String ESTADO_APROBADO = "Aprobado";
    private static final String ESTADO_PAGADO = "Pagado";
    private static final String ESTADO_RECHAZADO = "Rechazado";
    private static final String ESTADO_FALLIDO = "Fallido";

    /** Bloquean una nueva solicitud: ya hay dinero "reservado" para un retiro en curso. */
    private static final List<String> ESTADOS_EN_CURSO = List.of(ESTADO_PENDIENTE, ESTADO_APROBADO);

    /** Restan del saldo disponible: pedido, en proceso o ya pagado (nunca se vuelve a contar). */
    private static final List<String> ESTADOS_DESCUENTAN_SALDO = List.of(ESTADO_PENDIENTE, ESTADO_APROBADO, ESTADO_PAGADO);

    /** batch_status de PayPal que dejan la solicitud "en proceso": PayPal la resuelve más tarde (webhook o polling, fuera de alcance aquí). */
    private static final List<String> BATCH_STATUS_EN_PROCESO = List.of("PENDING", "UNCLAIMED", "PROCESSING");

    private final WithdrawalRequestRepository solicitudRetiroRepository;
    private final CreatorPaymentDetailsRepository datosPagoCreadorRepository;
    private final PaymentTransactionRepository transaccionPagoRepository;
    private final UserRepository usuarioRepository;
    private final PayPalClient payPalClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${retiros.monto-minimo:10.00}")
    private BigDecimal montoMinimo;

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idUsuarioCreador identificador del creador
     * @return el saldo disponible para retiro, el monto mínimo, y si ya tiene
     *         correo de PayPal configurado o una solicitud en curso
     */
    public CreatorBalanceResponse getBalance(Long idUsuarioCreador) {
        boolean tieneCorreo = datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuarioCreador).isPresent();
        boolean tienePendiente = solicitudRetiroRepository
                .existsByUsuarioCreadorIdUsuarioAndEstadoIn(idUsuarioCreador, ESTADOS_EN_CURSO);

        return CreatorBalanceResponse.builder()
                .saldoDisponible(calculateAvailableBalance(idUsuarioCreador))
                .montoMinimoRetiro(montoMinimo)
                .tieneCorreoPaypalConfigurado(tieneCorreo)
                .tieneSolicitudPendiente(tienePendiente)
                .build();
    }

    private BigDecimal calculateAvailableBalance(Long idUsuarioCreador) {
        BigDecimal totalEgresos = transaccionPagoRepository.sumEgresosPorCreador(idUsuarioCreador);
        BigDecimal enCurso = solicitudRetiroRepository
                .sumMontosEnCursoPorCreador(idUsuarioCreador, ESTADOS_DESCUENTAN_SALDO);
        return totalEgresos.subtract(enCurso);
    }

    @Override
    @Transactional
    @Auditable(accion = "RETIRO_SOLICITAR", modulo = AuditModule.FINANZAS,
            entidad = "solicitudes_retiro", idEntidad = "#resultado.idSolicitud",
            detalle = "{monto: #peticion.montoSolicitado}")
    /**
     * Crea una solicitud de retiro para un creador, en estado {@code Pendiente}.
     *
     * @param idUsuarioCreador identificador del creador que solicita el retiro
     * @param peticion monto solicitado
     * @return la solicitud creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el creador no tiene correo de
     *         PayPal configurado, si ya tiene una solicitud en curso, si el monto es menor al mínimo,
     *         o si supera el saldo disponible
     */
    public WithdrawalRequestResponse request(Long idUsuarioCreador, CreateWithdrawalRequest peticion) {
        CreatorPaymentDetails datosPago = datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuarioCreador)
                .orElseThrow(() -> new BusinessRuleException(
                        "Debes configurar tu correo de PayPal antes de request un retiro"));

        if (solicitudRetiroRepository.existsByUsuarioCreadorIdUsuarioAndEstadoIn(idUsuarioCreador, ESTADOS_EN_CURSO)) {
            throw new BusinessRuleException("Ya tienes una solicitud de retiro en curso");
        }

        BigDecimal monto = peticion.getMontoSolicitado();
        if (monto.compareTo(montoMinimo) < 0) {
            throw new BusinessRuleException("El monto mínimo de retiro es $" + montoMinimo);
        }

        BigDecimal saldoDisponible = calculateAvailableBalance(idUsuarioCreador);
        if (monto.compareTo(saldoDisponible) > 0) {
            throw new BusinessRuleException("El monto solicitado supera tu saldo disponible");
        }

        User usuario = usuarioRepository.findById(idUsuarioCreador)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado"));

        WithdrawalRequest solicitud = WithdrawalRequest.builder()
                .usuarioCreador(usuario)
                .montoSolicitado(monto)
                // Copia inmutable: si el creador cambia su correo después, no
                // debe alterar un retiro que ya quedó registrado con el viejo.
                .correoPaypalDestino(datosPago.getCorreoPaypal())
                .estado(ESTADO_PENDIENTE)
                .build();

        try {
            solicitud = solicitudRetiroRepository.save(solicitud);
        } catch (DataIntegrityViolationException e) {
            // Última línea de defensa: uq_solicitud_retiro_pendiente_por_creador
            // atrapa la carrera entre el existsBy de arriba y este insert si dos
            // solicitudes del mismo creador llegan casi simultáneas.
            throw new BusinessRuleException("Ya tienes una solicitud de retiro en curso");
        }

        log.info("Solicitud de retiro {} creada por creador {} por ${}",
                solicitud.getIdSolicitud(), idUsuarioCreador, monto);

        return mapear(solicitud);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idUsuarioCreador identificador del creador
     * @return las solicitudes de retiro del creador, más recientes primero
     */
    public List<WithdrawalRequestResponse> myRequests(Long idUsuarioCreador) {
        return solicitudRetiroRepository.findByUsuarioCreadorIdUsuarioOrderByFechaSolicitudDesc(idUsuarioCreador)
                .stream()
                .map(this::mapear)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Lista la cola de solicitudes de retiro filtradas, para el panel del auditor financiero.
     *
     * @param filtro estado, creador y rango de fechas a filtrar
     * @param pageable paginación y ordenamiento solicitados
     * @return la página de solicitudes que cumplen el filtro
     */
    public Page<WithdrawalRequestResponse> listQueue(WithdrawalRequestFilter filtro, Pageable pageable) {
        var spec = WithdrawalRequestSpecification.conFiltros(
                filtro.getEstado(), filtro.getIdUsuarioCreador(), filtro.getDesde(), filtro.getHasta());

        return solicitudRetiroRepository.findAll(spec, pageable).map(this::mapear);
    }

    @Override
    @Transactional
    @Auditable(accion = "RETIRO_APROBAR", modulo = AuditModule.FINANZAS,
            entidad = "solicitudes_retiro", idEntidad = "#idSolicitud")
    /**
     * Aprueba una solicitud de retiro pendiente y ejecuta el payout a PayPal;
     * el estado final depende de la respuesta de PayPal (pagado, en proceso o fallido).
     *
     * @param idSolicitud identificador de la solicitud a approve
     * @param idAdmin identificador del admin que decide
     * @return la solicitud con su estado final tras el intento de payout
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la solicitud o el admin no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la solicitud no está en estado {@code Pendiente}
     */
    public WithdrawalRequestResponse approve(Long idSolicitud, Long idAdmin) {
        WithdrawalRequest solicitud = getWithStatus(idSolicitud, ESTADO_PENDIENTE);
        User admin = getAdmin(idAdmin);

        solicitud.setAdminDecisor(admin);
        solicitud.setFechaDecision(LocalDateTime.now());
        executePayoutAndUpdateStatus(solicitud);

        solicitud = solicitudRetiroRepository.save(solicitud);
        log.info("Solicitud de retiro {} aprobada por admin {}: estado final {}",
                idSolicitud, idAdmin, solicitud.getEstado());
        return mapear(solicitud);
    }

    @Override
    @Transactional
    @Auditable(accion = "RETIRO_RECHAZAR", modulo = AuditModule.FINANZAS,
            entidad = "solicitudes_retiro", idEntidad = "#idSolicitud", detalle = "{nota: #notaAdmin}")
    /**
     * Rechaza una solicitud de retiro pendiente.
     *
     * @param idSolicitud identificador de la solicitud a reject
     * @param idAdmin identificador del admin que decide
     * @param notaAdmin justificación del rechazo, obligatoria
     * @return la solicitud ya marcada como rechazada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la solicitud o el admin no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si no se indica motivo, o si la
     *         solicitud no está en estado {@code Pendiente}
     */
    public WithdrawalRequestResponse reject(Long idSolicitud, Long idAdmin, String notaAdmin) {
        if (notaAdmin == null || notaAdmin.isBlank()) {
            throw new BusinessRuleException("Debes indicar un motivo para reject la solicitud");
        }

        WithdrawalRequest solicitud = getWithStatus(idSolicitud, ESTADO_PENDIENTE);
        User admin = getAdmin(idAdmin);

        solicitud.setEstado(ESTADO_RECHAZADO);
        solicitud.setNotaAdmin(notaAdmin);
        solicitud.setAdminDecisor(admin);
        solicitud.setFechaDecision(LocalDateTime.now());

        solicitud = solicitudRetiroRepository.save(solicitud);
        log.info("Solicitud de retiro {} rechazada por admin {}", idSolicitud, idAdmin);
        return mapear(solicitud);
    }

    @Override
    @Transactional
    @Auditable(accion = "RETIRO_REINTENTAR", modulo = AuditModule.FINANZAS,
            entidad = "solicitudes_retiro", idEntidad = "#idSolicitud")
    /**
     * Reintenta el payout de una solicitud que había fallado.
     *
     * @param idSolicitud identificador de la solicitud a retry
     * @param idAdmin identificador del admin que decide
     * @return la solicitud con su estado final tras el nuevo intento de payout
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la solicitud o el admin no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la solicitud no está en estado {@code Fallido}
     */
    public WithdrawalRequestResponse retry(Long idSolicitud, Long idAdmin) {
        WithdrawalRequest solicitud = getWithStatus(idSolicitud, ESTADO_FALLIDO);
        User admin = getAdmin(idAdmin);

        solicitud.setAdminDecisor(admin);
        solicitud.setFechaDecision(LocalDateTime.now());
        executePayoutAndUpdateStatus(solicitud);

        solicitud = solicitudRetiroRepository.save(solicitud);
        log.info("Solicitud de retiro {} reintentada por admin {}: estado final {}",
                idSolicitud, idAdmin, solicitud.getEstado());
        return mapear(solicitud);
    }

    // ── PayPal Payouts ───────────────────────────────────────────────────────

    /**
     * Ejecuta el payout y deja la solicitud en el estado que corresponda según
     * la respuesta de PayPal. Nunca propaga la excepción: un fallo de PayPal es
     * un resultado de negocio válido (ESTADO_FALLIDO), no un error del sistema
     * que deba abortar la transacción y perder el registro de qué pasó.
     */
    private void executePayoutAndUpdateStatus(WithdrawalRequest solicitud) {
        try {
            JsonNode respuesta = executePayout(solicitud);
            String estadoLote = respuesta.path("batch_header").path("batch_status").asText();
            String payoutBatchId = respuesta.path("batch_header").path("payout_batch_id").asText(null);
            solicitud.setIdPayoutPaypal(payoutBatchId);

            if ("SUCCESS".equals(estadoLote)) {
                solicitud.setEstado(ESTADO_PAGADO);
                solicitud.setFechaPago(LocalDateTime.now());
                solicitud.setMensajeError(null);
            } else if (BATCH_STATUS_EN_PROCESO.contains(estadoLote)) {
                solicitud.setEstado(ESTADO_APROBADO);
                solicitud.setMensajeError(null);
            } else {
                solicitud.setEstado(ESTADO_FALLIDO);
                solicitud.setMensajeError("PayPal devolvió el estado " + estadoLote);
            }
        } catch (HttpStatusCodeException e) {
            solicitud.setEstado(ESTADO_FALLIDO);
            solicitud.setMensajeError("Error de PayPal (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            log.error("Error ejecutando el payout de la solicitud {}: {}",
                    solicitud.getIdSolicitud(), e.getResponseBodyAsString());
        } catch (Exception e) {
            solicitud.setEstado(ESTADO_FALLIDO);
            solicitud.setMensajeError("Error al comunicarse con PayPal: " + e.getMessage());
            log.error("Error ejecutando el payout de la solicitud {}", solicitud.getIdSolicitud(), e);
        }
    }

    private JsonNode executePayout(WithdrawalRequest solicitud) {
        ObjectNode raiz = objectMapper.createObjectNode();

        // sender_batch_id idempotente: un reintento sobre la misma solicitud
        // reenvía el mismo id, así que si PayPal ya había procesado el intento
        // anterior (p. ej. un timeout de red que ocultó una respuesta SUCCESS),
        // deduplica en vez de pagar dos veces.
        raiz.putObject("sender_batch_header")
                .put("sender_batch_id", "retiro-" + solicitud.getIdSolicitud())
                .put("email_subject", "Has recibido un pago de ARTISYNC");

        ObjectNode item = raiz.putArray("items").addObject();
        item.putObject("amount")
                .put("value", solicitud.getMontoSolicitado().toPlainString())
                .put("currency", "USD");
        item.put("receiver", solicitud.getCorreoPaypalDestino());
        item.put("note", "Pago de retiro ARTISYNC #" + solicitud.getIdSolicitud());
        item.put("sender_item_id", "item-" + solicitud.getIdSolicitud());

        return payPalClient.llamarPayPal("/v1/payments/payouts", HttpMethod.POST, raiz);
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    /** Con bloqueo pesimista (ver WithdrawalRequestRepository.findByIdParaActualizar): serializa decisiones concurrentes. */
    private WithdrawalRequest getWithStatus(Long idSolicitud, String estadoEsperado) {
        WithdrawalRequest solicitud = solicitudRetiroRepository.findByIdParaActualizar(idSolicitud)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de retiro no encontrada"));

        if (!estadoEsperado.equals(solicitud.getEstado())) {
            throw new BusinessRuleException(
                    "La solicitud no está en estado " + estadoEsperado + " (estado actual: " + solicitud.getEstado() + ")");
        }
        return solicitud;
    }

    private User getAdmin(Long idAdmin) {
        return usuarioRepository.findById(idAdmin)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado"));
    }

    private WithdrawalRequestResponse mapear(WithdrawalRequest s) {
        User creador = s.getUsuarioCreador();
        return WithdrawalRequestResponse.builder()
                .idSolicitud(s.getIdSolicitud())
                .idUsuarioCreador(creador.getIdUsuario())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                .montoSolicitado(s.getMontoSolicitado())
                .correoPaypalDestino(s.getCorreoPaypalDestino())
                .estado(s.getEstado())
                .idPayoutPaypal(s.getIdPayoutPaypal())
                .notaAdmin(s.getNotaAdmin())
                .mensajeError(s.getMensajeError())
                .fechaSolicitud(s.getFechaSolicitud())
                .fechaDecision(s.getFechaDecision())
                .fechaPago(s.getFechaPago())
                .build();
    }
}
