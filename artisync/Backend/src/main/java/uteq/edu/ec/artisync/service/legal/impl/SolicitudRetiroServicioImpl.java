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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroSolicitudRetiro;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionSolicitudRetiro;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaSaldoCreador;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaSolicitudRetiro;
import uteq.edu.ec.artisync.entity.legal.SolicitudRetiro;
import uteq.edu.ec.artisync.entity.perfil.DatosPagoCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.SolicitudRetiroRepository;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;
import uteq.edu.ec.artisync.repository.perfil.DatosPagoCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.legal.ISolicitudRetiroServicio;
import uteq.edu.ec.artisync.specification.legal.SolicitudRetiroSpecification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * El cliente HTTP de PayPal (llamarPayPal/obtenerAccessToken/getPayPalBaseUrl,
 * más abajo) está deliberadamente duplicado desde PagoServicioImpl en vez de
 * extraído a un componente compartido: este módulo es nuevo y ese código de
 * pagos ya está probado en producción, así que se prefirió no arriesgarlo con
 * un refactor en el mismo cambio. La extracción de un PayPalClient común queda
 * como mejora explícita post-lanzamiento (ver plan de retiros, Fase 4).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SolicitudRetiroServicioImpl implements ISolicitudRetiroServicio {

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

    private final SolicitudRetiroRepository solicitudRetiroRepository;
    private final DatosPagoCreadorRepository datosPagoCreadorRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;
    private final UsuarioRepository usuarioRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** No es `final` para que las pruebas puedan sustituirlo (mismo motivo que en PagoServicioImpl). */
    private RestTemplate restTemplate = new RestTemplate();

    @Value("${retiros.monto-minimo:10.00}")
    private BigDecimal montoMinimo;

    @Value("${paypal.client-id:sandbox_client_id}")
    private String paypalClientId;

    @Value("${paypal.client-secret:sandbox_client_secret}")
    private String paypalClientSecret;

    @Value("${paypal.mode:sandbox}")
    private String paypalMode;

    private String getPayPalBaseUrl() {
        return "sandbox".equalsIgnoreCase(paypalMode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaSaldoCreador obtenerSaldo(Long idUsuarioCreador) {
        boolean tieneCorreo = datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuarioCreador).isPresent();
        boolean tienePendiente = solicitudRetiroRepository
                .existsByUsuarioCreadorIdUsuarioAndEstadoIn(idUsuarioCreador, ESTADOS_EN_CURSO);

        return RespuestaSaldoCreador.builder()
                .saldoDisponible(calcularSaldoDisponible(idUsuarioCreador))
                .montoMinimoRetiro(montoMinimo)
                .tieneCorreoPaypalConfigurado(tieneCorreo)
                .tieneSolicitudPendiente(tienePendiente)
                .build();
    }

    private BigDecimal calcularSaldoDisponible(Long idUsuarioCreador) {
        BigDecimal totalEgresos = transaccionPagoRepository.sumEgresosPorCreador(idUsuarioCreador);
        BigDecimal enCurso = solicitudRetiroRepository
                .sumMontosEnCursoPorCreador(idUsuarioCreador, ESTADOS_DESCUENTAN_SALDO);
        return totalEgresos.subtract(enCurso);
    }

    @Override
    @Transactional
    @Auditable(accion = "RETIRO_SOLICITAR", modulo = ModuloAuditoria.FINANZAS,
            entidad = "solicitudes_retiro", idEntidad = "#resultado.idSolicitud",
            detalle = "{monto: #peticion.montoSolicitado}")
    public RespuestaSolicitudRetiro solicitar(Long idUsuarioCreador, PeticionSolicitudRetiro peticion) {
        DatosPagoCreador datosPago = datosPagoCreadorRepository.findByUsuarioIdUsuario(idUsuarioCreador)
                .orElseThrow(() -> new ExcepcionReglaNegocio(
                        "Debes configurar tu correo de PayPal antes de solicitar un retiro"));

        if (solicitudRetiroRepository.existsByUsuarioCreadorIdUsuarioAndEstadoIn(idUsuarioCreador, ESTADOS_EN_CURSO)) {
            throw new ExcepcionReglaNegocio("Ya tienes una solicitud de retiro en curso");
        }

        BigDecimal monto = peticion.getMontoSolicitado();
        if (monto.compareTo(montoMinimo) < 0) {
            throw new ExcepcionReglaNegocio("El monto mínimo de retiro es $" + montoMinimo);
        }

        BigDecimal saldoDisponible = calcularSaldoDisponible(idUsuarioCreador);
        if (monto.compareTo(saldoDisponible) > 0) {
            throw new ExcepcionReglaNegocio("El monto solicitado supera tu saldo disponible");
        }

        Usuario usuario = usuarioRepository.findById(idUsuarioCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado"));

        SolicitudRetiro solicitud = SolicitudRetiro.builder()
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
            throw new ExcepcionReglaNegocio("Ya tienes una solicitud de retiro en curso");
        }

        log.info("Solicitud de retiro {} creada por creador {} por ${}",
                solicitud.getIdSolicitud(), idUsuarioCreador, monto);

        return mapear(solicitud);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSolicitudRetiro> misSolicitudes(Long idUsuarioCreador) {
        return solicitudRetiroRepository.findByUsuarioCreadorIdUsuarioOrderByFechaSolicitudDesc(idUsuarioCreador)
                .stream()
                .map(this::mapear)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaSolicitudRetiro> listarCola(FiltroSolicitudRetiro filtro, Pageable pageable) {
        var spec = SolicitudRetiroSpecification.conFiltros(
                filtro.getEstado(), filtro.getIdUsuarioCreador(), filtro.getDesde(), filtro.getHasta());

        return solicitudRetiroRepository.findAll(spec, pageable).map(this::mapear);
    }

    @Override
    @Transactional
    @Auditable(accion = "RETIRO_APROBAR", modulo = ModuloAuditoria.FINANZAS,
            entidad = "solicitudes_retiro", idEntidad = "#idSolicitud")
    public RespuestaSolicitudRetiro aprobar(Long idSolicitud, Long idAdmin) {
        SolicitudRetiro solicitud = obtenerConEstado(idSolicitud, ESTADO_PENDIENTE);
        Usuario admin = obtenerAdmin(idAdmin);

        solicitud.setAdminDecisor(admin);
        solicitud.setFechaDecision(LocalDateTime.now());
        ejecutarPayoutYActualizarEstado(solicitud);

        solicitud = solicitudRetiroRepository.save(solicitud);
        log.info("Solicitud de retiro {} aprobada por admin {}: estado final {}",
                idSolicitud, idAdmin, solicitud.getEstado());
        return mapear(solicitud);
    }

    @Override
    @Transactional
    @Auditable(accion = "RETIRO_RECHAZAR", modulo = ModuloAuditoria.FINANZAS,
            entidad = "solicitudes_retiro", idEntidad = "#idSolicitud", detalle = "{nota: #notaAdmin}")
    public RespuestaSolicitudRetiro rechazar(Long idSolicitud, Long idAdmin, String notaAdmin) {
        if (notaAdmin == null || notaAdmin.isBlank()) {
            throw new ExcepcionReglaNegocio("Debes indicar un motivo para rechazar la solicitud");
        }

        SolicitudRetiro solicitud = obtenerConEstado(idSolicitud, ESTADO_PENDIENTE);
        Usuario admin = obtenerAdmin(idAdmin);

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
    @Auditable(accion = "RETIRO_REINTENTAR", modulo = ModuloAuditoria.FINANZAS,
            entidad = "solicitudes_retiro", idEntidad = "#idSolicitud")
    public RespuestaSolicitudRetiro reintentar(Long idSolicitud, Long idAdmin) {
        SolicitudRetiro solicitud = obtenerConEstado(idSolicitud, ESTADO_FALLIDO);
        Usuario admin = obtenerAdmin(idAdmin);

        solicitud.setAdminDecisor(admin);
        solicitud.setFechaDecision(LocalDateTime.now());
        ejecutarPayoutYActualizarEstado(solicitud);

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
    private void ejecutarPayoutYActualizarEstado(SolicitudRetiro solicitud) {
        try {
            JsonNode respuesta = ejecutarPayout(solicitud);
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

    private JsonNode ejecutarPayout(SolicitudRetiro solicitud) {
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

        return llamarPayPal("/v1/payments/payouts", HttpMethod.POST, raiz);
    }

    /** Llamada autenticada a la API de PayPal. `cuerpo` null para GET. */
    private JsonNode llamarPayPal(String ruta, HttpMethod metodo, JsonNode cuerpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(obtenerAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> peticion = new HttpEntity<>(cuerpo != null ? cuerpo.toString() : null, headers);

        ResponseEntity<String> respuesta = restTemplate.exchange(
                getPayPalBaseUrl() + ruta, metodo, peticion, String.class);

        try {
            return objectMapper.readTree(respuesta.getBody());
        } catch (Exception e) {
            throw new ExcepcionReglaNegocio("Respuesta ilegible de PayPal en " + ruta);
        }
    }

    private String obtenerAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = Base64.getEncoder().encodeToString(
                (paypalClientId + ":" + paypalClientSecret).getBytes());
        headers.set("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                getPayPalBaseUrl() + "/v1/oauth2/token",
                HttpMethod.POST, request, Map.class);

        return (String) response.getBody().get("access_token");
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    /** Con bloqueo pesimista (ver SolicitudRetiroRepository.findByIdParaActualizar): serializa decisiones concurrentes. */
    private SolicitudRetiro obtenerConEstado(Long idSolicitud, String estadoEsperado) {
        SolicitudRetiro solicitud = solicitudRetiroRepository.findByIdParaActualizar(idSolicitud)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Solicitud de retiro no encontrada"));

        if (!estadoEsperado.equals(solicitud.getEstado())) {
            throw new ExcepcionReglaNegocio(
                    "La solicitud no está en estado " + estadoEsperado + " (estado actual: " + solicitud.getEstado() + ")");
        }
        return solicitud;
    }

    private Usuario obtenerAdmin(Long idAdmin) {
        return usuarioRepository.findById(idAdmin)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado"));
    }

    private RespuestaSolicitudRetiro mapear(SolicitudRetiro s) {
        Usuario creador = s.getUsuarioCreador();
        return RespuestaSolicitudRetiro.builder()
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
