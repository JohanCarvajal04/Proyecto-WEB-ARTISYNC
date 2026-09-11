package uteq.edu.ec.artisync.service.legal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.respuesta.legal.PaymentResponse;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;
import uteq.edu.ec.artisync.entity.legal.PaymentTransaction;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.legal.EscrowPaymentRepository;
import uteq.edu.ec.artisync.repository.legal.PaymentTransactionRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;
import uteq.edu.ec.artisync.service.legal.IPaymentService;
import uteq.edu.ec.artisync.service.legal.IRevisionTicketPaymentService;
import uteq.edu.ec.artisync.service.shared.paypal.PayPalClient;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements IPaymentService {

    /** Estados de `pagos_garantia.estado_fondos`. */
    private static final String FONDOS_PENDIENTE = "Pendiente";
    private static final String FONDOS_RETENIDO = "Retenido";
    private static final String FONDOS_LIBERADO = "Liberado";
    private static final String FONDOS_REEMBOLSADO = "Reembolsado";
    private static final String FONDOS_REEMBOLSO_FALLIDO = "ReembolsoFallido";

    private static final String EVENTO_ORDEN_APROBADA = "CHECKOUT.ORDER.APPROVED";
    private static final String EVENTO_CAPTURA_COMPLETADA = "PAYMENT.CAPTURE.COMPLETED";

    /** Valores válidos de {@code accionFondos} en cancelarPedidoConFondosRetenidos. */
    private static final String ACCION_REEMBOLSAR = "REEMBOLSAR";
    private static final String ACCION_LIBERAR = "LIBERAR";

    private final EscrowPaymentRepository pagoGarantiaRepository;
    private final ContractRepository contratoRepository;
    private final PaymentTransactionRepository transaccionPagoRepository;
    private final NotificacionService notificacionService;
    private final PayPalClient payPalClient;
    private final IRevisionTicketPaymentService pagoTicketRevisionServicio;

    /**
     * Mapper propio, no el del contexto: el contrato con PayPal no debe verse
     * afectado por personalizaciones de serialización de la aplicación.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${paypal.webhook-id:}")
    private String paypalWebhookId;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Misma fuente que DeliverableServiceImpl (comentario allí: "Fuente unica
     * con FinancialReportServiceImpl"): la liberación por cancelación
     * (accionFondos=LIBERAR) reparte los fondos con la misma tasa que una
     * aprobación normal de entrega.
     */
    @Value("${plataforma.comision-tasa:0.10}")
    private BigDecimal tasaComision;

    // ── Creación de la orden ─────────────────────────────────────────────────

    @Override
    @Transactional
    @Auditable(accion = "PAGO_ORDEN_CREAR", modulo = AuditModule.FINANZAS,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{monto: #monto}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idCliente identificador unico que referencia de manera univoca al registro
     * @param monto parametro requerido para la correcta ejecucion del procedimiento
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public PaymentResponse crearOrdenPayPal(Long idPedido, Long idCliente, BigDecimal monto) {
        Contract contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No existe contrato para el pedido"));

        // @PreAuthorize solo exige el rol CLIENTE, no que el pedido sea suyo:
        // sin esto, cualquier cliente autenticado podía crear (y ver el estado
        // de) la orden de pago de un pedido ajeno.
        if (!contrato.getPedido().getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new BusinessRuleException("Solo el cliente del pedido puede iniciar el pago");
        }

        if (contrato.getHashFirmaCreador() == null || contrato.getHashFirmaCliente() == null) {
            throw new BusinessRuleException("El contrato debe estar firmado por ambas partes antes de realizar el pago");
        }

        Optional<EscrowPayment> existente = pagoGarantiaRepository.findByContratoIdContrato(contrato.getIdContrato());

        // Un pago ya confirmado no se vuelve a cobrar.
        if (existente.isPresent() && !FONDOS_PENDIENTE.equalsIgnoreCase(existente.get().getEstadoFondos())) {
            throw new BusinessRuleException(
                    "Este pedido ya tiene un pago en estado " + existente.get().getEstadoFondos());
        }

        BigDecimal montoFinal = monto != null ? monto : contrato.getPedido().getPrecioPactado();

        try {
            // La respuesta de creación ya trae el id y los links: no hace falta
            // un GET posterior para leer el approvalUrl.
            JsonNode orden = crearOrdenEnPayPal(idPedido, montoFinal);
            String orderId = orden.path("id").asText();
            String approvalUrl = extraerApprovalUrl(orden);

            // Reutiliza la fila pendiente en lugar de acumular una por clic.
            EscrowPayment pago = existente.orElseGet(() -> EscrowPayment.builder().contrato(contrato).build());
            pago.setIdOrdenPaypal(orderId);
            pago.setMontoRetenido(montoFinal);
            pago.setEstadoFondos(FONDOS_PENDIENTE);
            try {
                pago = pagoGarantiaRepository.save(pago);
            } catch (DataIntegrityViolationException e) {
                // Carrera entre el findByContratoIdContrato de arriba y este
                // insert/update: id_contrato es UNIQUE, así que dos clics casi
                // simultáneos sobre el mismo contrato no pueden colar dos filas
                // en pagos_garantia. Mismo patrón que
                // WithdrawalRequestServiceImpl.solicitar.
                throw new BusinessRuleException("Este pedido ya tiene un pago en curso");
            }

            log.info("Orden PayPal {} creada para pedido {} por ${}", orderId, idPedido, montoFinal);

            return PaymentResponse.builder()
                    .idPago(pago.getIdPago())
                    .idContrato(contrato.getIdContrato())
                    .idOrdenPaypal(orderId)
                    .montoRetenido(montoFinal)
                    .estadoFondos(FONDOS_PENDIENTE)
                    .approvalUrl(approvalUrl)
                    .build();

        } catch (BusinessRuleException | ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al crear orden PayPal para pedido {}", idPedido, e);
            throw new BusinessRuleException("Error al comunicarse con PayPal: " + e.getMessage());
        }
    }

    private JsonNode crearOrdenEnPayPal(Long idPedido, BigDecimal montoFinal) {
        // El retorno apunta a la propia pantalla de pago del pedido: allí se
        // consulta el estado real contra el backend, que es más fiable que
        // confiar en el parámetro con el que PayPal redirige.
        String retorno = frontendUrl + "/legal/pago/" + idPedido;

        ObjectNode raiz = objectMapper.createObjectNode();
        raiz.put("intent", "CAPTURE");

        ObjectNode unidad = raiz.putArray("purchase_units").addObject();
        unidad.put("description", "ARTISYNC - Order #" + idPedido);
        unidad.putObject("amount")
                .put("currency_code", "USD")
                .put("value", montoFinal.toPlainString());

        raiz.putObject("application_context")
                .put("return_url", retorno)
                .put("cancel_url", retorno);

        return payPalClient.llamarPayPal("/v2/checkout/orders", HttpMethod.POST, raiz);
    }

    private String extraerApprovalUrl(JsonNode orden) {
        for (JsonNode enlace : orden.path("links")) {
            if ("approve".equals(enlace.path("rel").asText())) {
                return enlace.path("href").asText();
            }
        }
        return "";
    }

    // ── Webhook ──────────────────────────────────────────────────────────────

    /**
     * Procesa una notificación de PayPal.
     *
     * <p>Antes esto marcaba los fondos como retenidos sin verificar la firma y
     * extrayendo el id con {@code indexOf("\"id\":\"")}, que en un webhook real
     * devuelve el id del <em>evento</em>, no el de la orden. El resultado era el
     * peor posible: fallaba con las notificaciones legítimas y funcionaba con
     * cualquier POST falsificado, porque el atacante sí ponía el id donde el
     * parser lo buscaba.
     *
     * <p>Ahora se verifica la firma contra PayPal (RNF-14, fail-closed) y se
     * captura la orden de verdad: con {@code intent=CAPTURE}, la aprobación solo
     * autoriza — sin la llamada a /capture el dinero nunca sale de la cuenta del
     * cliente y el escrow era ficticio.
     */
    @Override
    @Transactional
    // Jamás #payload en el detalle: es el cuerpo crudo del webhook y puede
    // llevar datos de la orden completos. Solo se registra transmissionId,
    // suficiente para correlacionar con los logs de PayPal si hace falta.
    @Auditable(accion = "PAGO_WEBHOOK_RECIBIR", modulo = AuditModule.FINANZAS,
            correoActor = "'sistema:paypal'",
            detalle = "{transmissionId: #transmissionId}")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     * @param payload carga util del webhook
     * @param transmissionId id de transmision
     * @param transmissionTime tiempo de transmision
     * @param certUrl url del certificado
     * @param authAlgo algoritmo de autenticacion
     * @param transmissionSig firma de transmision
     * @param webhookId id del webhook
     */
    public void procesarWebhookPayPal(String payload, String transmissionId, String transmissionTime,
                                      String transmissionSig, String certUrl, String authAlgo, String authVersion) {
        JsonNode evento;
        try {
            evento = objectMapper.readTree(payload);
        } catch (Exception e) {
            log.warn("Webhook PayPal con payload ilegible; se descarta");
            return;
        }

        if (!firmaVerificada(evento, transmissionId, transmissionTime, transmissionSig, certUrl, authAlgo)) {
            log.warn("Webhook PayPal rechazado: firma no verificada (transmissionId={})", transmissionId);
            return;
        }

        String tipoEvento = evento.path("event_type").asText();
        JsonNode recurso = evento.path("resource");

        String orderId = switch (tipoEvento) {
            case EVENTO_ORDEN_APROBADA -> recurso.path("id").asText(null);
            // En una captura, `resource.id` es el id de la captura; el de la
            // orden viaja en supplementary_data.
            case EVENTO_CAPTURA_COMPLETADA -> recurso
                    .path("supplementary_data").path("related_ids").path("order_id").asText(null);
            default -> null;
        };

        if (orderId == null || orderId.isBlank()) {
            log.debug("Webhook PayPal '{}' ignorado: sin id de orden aplicable", tipoEvento);
            return;
        }

        EscrowPayment pago = pagoGarantiaRepository.findByIdOrdenPaypal(orderId).orElse(null);
        if (pago == null) {
            // REQ-F-022b/c: la orden puede ser el cargo adicional de un ticket
            // de revisión, no un pago de garantía. La verificación de firma y
            // la resolución del orderId de arriba no cambian para ninguno de
            // los dos casos.
            if (pagoTicketRevisionServicio.procesarWebhookOrden(orderId, tipoEvento)) {
                return;
            }
            log.warn("Webhook PayPal para la orden {}, que no corresponde a ningún pago registrado", orderId);
            return;
        }

        // Idempotencia: PayPal reintenta por diseño y ambos eventos pueden
        // llegar por la misma orden. Sin esto se duplicaba la transacción.
        if (!FONDOS_PENDIENTE.equalsIgnoreCase(pago.getEstadoFondos())) {
            log.info("Webhook PayPal duplicado para la orden {}: el pago ya está en {}",
                    orderId, pago.getEstadoFondos());
            return;
        }

        if (EVENTO_ORDEN_APROBADA.equals(tipoEvento) && !capturarOrden(orderId)) {
            log.error("No se pudo capturar la orden {}; los fondos siguen pendientes", orderId);
            return;
        }

        pago.setEstadoFondos(FONDOS_RETENIDO);
        pagoGarantiaRepository.save(pago);

        transaccionPagoRepository.save(PaymentTransaction.builder()
                .pago(pago)
                .tipoTransaccion("Ingreso")
                .monto(pago.getMontoRetenido())
                .build());

        log.info("Pago {} confirmado y capturado. Fondos retenidos: ${}",
                pago.getIdPago(), pago.getMontoRetenido());

        Order pedido = pago.getContrato().getPedido();
        String mensaje = "El pago de tu pedido \"" + pedido.getServicio().getTituloServicio()
                + "\" fue confirmado. Los fondos quedan en garantía hasta la aprobación de la entrega.";
        notificacionService.notificar(pedido.getUsuarioCliente(), "PAGO_CONFIRMADO", mensaje);
        notificacionService.notificar(pedido.getServicio().getPerfil().getUsuario(), "PAGO_CONFIRMADO",
                "Se confirmó el pago de garantía para el pedido \"" + pedido.getServicio().getTituloServicio() + "\".");
    }

    /**
     * Verificación de firma contra PayPal (RNF-14). Fail-closed: cualquier duda
     * —error de red, webhook-id sin configurar, respuesta distinta de SUCCESS—
     * se resuelve rechazando la notificación. El endpoint es público, así que
     * esta comprobación es lo único que separa un aviso real de un POST
     * falsificado que marque un pedido como pagado.
     */
    private boolean firmaVerificada(JsonNode evento, String transmissionId, String transmissionTime,
                                    String transmissionSig, String certUrl, String authAlgo) {
        if (paypalWebhookId == null || paypalWebhookId.isBlank()) {
            log.error("paypal.webhook-id sin configurar: no se puede verificar la firma del webhook");
            return false;
        }
        if (transmissionId == null || transmissionSig == null || certUrl == null || authAlgo == null) {
            log.warn("Webhook PayPal sin las cabeceras de firma");
            return false;
        }

        try {
            ObjectNode peticion = objectMapper.createObjectNode();
            peticion.put("auth_algo", authAlgo);
            peticion.put("cert_url", certUrl);
            peticion.put("transmission_id", transmissionId);
            peticion.put("transmission_sig", transmissionSig);
            peticion.put("transmission_time", transmissionTime);
            peticion.put("webhook_id", paypalWebhookId);
            peticion.set("webhook_event", evento);

            JsonNode respuesta = payPalClient.llamarPayPal("/v1/notifications/verify-webhook-signature",
                    HttpMethod.POST, peticion);

            return "SUCCESS".equals(respuesta.path("verification_status").asText());
        } catch (Exception e) {
            log.error("Error verificando la firma del webhook PayPal", e);
            return false;
        }
    }

    /**
     * Captura la orden aprobada. Devuelve `true` si el dinero quedó cobrado.
     *
     * <p>Una orden ya capturada se trata como éxito: es el caso normal cuando
     * PayPal reintenta la notificación después de que la primera sí capturara.
     */
    private boolean capturarOrden(String orderId) {
        try {
            JsonNode respuesta = payPalClient.llamarPayPal("/v2/checkout/orders/" + orderId + "/capture",
                    HttpMethod.POST, objectMapper.createObjectNode());
            String estado = respuesta.path("status").asText();
            if (!"COMPLETED".equals(estado)) {
                log.error("La captura de la orden {} devolvió estado {}", orderId, estado);
                return false;
            }
            return true;
        } catch (HttpStatusCodeException e) {
            if (e.getResponseBodyAsString().contains("ORDER_ALREADY_CAPTURED")) {
                log.info("La orden {} ya estaba capturada", orderId);
                return true;
            }
            log.error("Error capturando la orden {}: {}", orderId, e.getResponseBodyAsString());
            return false;
        } catch (Exception e) {
            log.error("Error capturando la orden {}", orderId, e);
            return false;
        }
    }

    // ── Consulta ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public PaymentResponse obtenerEstadoPago(Long idPedido, Long idUsuario) {
        Contract contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No existe contrato para el pedido"));

        // El controlador solo exige isAuthenticated(): sin esta verificación,
        // cualquier usuario logueado podía consultar el monto retenido y el id
        // de orden de PayPal de un pedido ajeno.
        Order pedidoDelContrato = contrato.getPedido();
        boolean esCliente = pedidoDelContrato.getUsuarioCliente().getIdUsuario().equals(idUsuario);
        boolean esCreador = pedidoDelContrato.getServicio().getPerfil().getUsuario().getIdUsuario().equals(idUsuario);
        if (!esCliente && !esCreador) {
            throw new BusinessRuleException("No tiene acceso al pago de este pedido");
        }

        EscrowPayment pago = pagoGarantiaRepository.findByContratoIdContrato(contrato.getIdContrato())
                .orElseThrow(() -> new ResourceNotFoundException("No existe pago registrado para este pedido"));

        return PaymentResponse.builder()
                .idPago(pago.getIdPago())
                .idContrato(contrato.getIdContrato())
                .idOrdenPaypal(pago.getIdOrdenPaypal())
                .montoRetenido(pago.getMontoRetenido())
                .estadoFondos(pago.getEstadoFondos())
                .mensajeError(pago.getMensajeError())
                .build();
    }

    // ── Cancelación con fondos retenidos (REQ-NF-019) ───────────────────────────

    /**
     * Cancela un pedido cuyos fondos ya están retenidos en escrow, reembolsando
     * al cliente vía PayPal (por defecto) o liberando los fondos al creador
     * (solo administrador, para disputas donde el trabajo ya se realizó).
     *
     * <p>Antes de esto no existía ninguna función para cancelar un pedido con
     * fondos ya retenidos: el dinero quedaba retenido indefinidamente sin
     * camino de salida.
     */
    @Override
    @Transactional
    @Auditable(accion = "PAGO_CANCELAR", modulo = AuditModule.FINANZAS,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{accionFondos: #accionFondos, motivo: #motivo}")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     * @param idPedido id del pedido
     * @param idUsuarioSolicitante id del usuario solicitante
     * @param motivo motivo de la cancelacion
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     */
    public PaymentResponse cancelarPedidoConFondosRetenidos(Long idPedido, Long idUsuarioSolicitante,
                                                           String accionFondos, String motivo) {
        Contract contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No existe contrato para el pedido"));

        Order pedido = contrato.getPedido();
        boolean esCliente = pedido.getUsuarioCliente().getIdUsuario().equals(idUsuarioSolicitante);
        boolean esAdmin = tienePermisoAdmin();
        // El creador nunca puede cancelar-y-cobrar su propio reembolso o
        // liberación: solo quien pagó, o un administrador que arbitra la disputa.
        if (!esCliente && !esAdmin) {
            throw new BusinessRuleException("Solo el cliente del pedido o un administrador pueden cancelar este pago");
        }

        String accion = (accionFondos == null || accionFondos.isBlank())
                ? ACCION_REEMBOLSAR : accionFondos.trim().toUpperCase();
        if (!ACCION_REEMBOLSAR.equals(accion) && !ACCION_LIBERAR.equals(accion)) {
            throw new BusinessRuleException("accionFondos debe ser REEMBOLSAR o LIBERAR");
        }
        if (ACCION_LIBERAR.equals(accion) && !esAdmin) {
            throw new BusinessRuleException("Solo un administrador puede liberar los fondos sin reembolsarlos");
        }

        EscrowPayment pago = pagoGarantiaRepository.findByContratoIdContratoParaActualizar(contrato.getIdContrato())
                .orElseThrow(() -> new ResourceNotFoundException("No existe pago registrado para este pedido"));

        // Reintentable: un reembolso que falló localmente (ReembolsoFallido)
        // puede reintentarse llamando este mismo endpoint, sin un endpoint
        // "reintentar" aparte (mismo enfoque que WithdrawalRequestServiceImpl).
        if (!FONDOS_RETENIDO.equalsIgnoreCase(pago.getEstadoFondos())
                && !FONDOS_REEMBOLSO_FALLIDO.equalsIgnoreCase(pago.getEstadoFondos())) {
            throw new BusinessRuleException(
                    "No se puede cancelar: el pago no está retenido (estado actual: " + pago.getEstadoFondos() + ")");
        }

        if (ACCION_REEMBOLSAR.equals(accion)) {
            ejecutarReembolso(pago);
        } else {
            ejecutarLiberacionPorCancelacion(pago);
        }
        // Sin reasignar desde el retorno (a diferencia de crearOrdenPayPal,
        // donde 'pago' puede ser una entidad recién construida sin id
        // generado todavía): aquí 'pago' ya viene de una carga existente con
        // id, y save() sobre una entidad administrada devuelve la misma
        // instancia ya mutada in situ por ejecutarReembolso/ejecutarLiberacionPorCancelacion.
        pagoGarantiaRepository.save(pago);

        log.info("Order {} cancelado con fondos retenidos por usuario {}: accion={}, estado final={}, motivo={}",
                idPedido, idUsuarioSolicitante, accion, pago.getEstadoFondos(), motivo);

        String tituloServicio = pedido.getServicio().getTituloServicio();
        String mensaje = "El pedido \"" + tituloServicio + "\" fue cancelado. Estado del pago: "
                + pago.getEstadoFondos() + ".";
        notificacionService.notificar(pedido.getUsuarioCliente(), "PEDIDO_CANCELADO", mensaje);
        notificacionService.notificar(pedido.getServicio().getPerfil().getUsuario(), "PEDIDO_CANCELADO", mensaje);

        return PaymentResponse.builder()
                .idPago(pago.getIdPago())
                .idContrato(contrato.getIdContrato())
                .idOrdenPaypal(pago.getIdOrdenPaypal())
                .montoRetenido(pago.getMontoRetenido())
                .estadoFondos(pago.getEstadoFondos())
                .mensajeError(pago.getMensajeError())
                .build();
    }

    /**
     * Reembolsa vía PayPal. Nunca propaga la excepción: un fallo de PayPal es
     * un resultado de negocio válido (ReembolsoFallido, reintentable), no un
     * error del sistema que deba abortar la transacción — mismo patrón que
     * WithdrawalRequestServiceImpl.ejecutarPayoutYActualizarEstado.
     */
    private void ejecutarReembolso(EscrowPayment pago) {
        try {
            String idCaptura = obtenerIdCaptura(pago.getIdOrdenPaypal());
            if (idCaptura == null) {
                pago.setEstadoFondos(FONDOS_REEMBOLSO_FALLIDO);
                pago.setMensajeError("No se encontró una captura completada para la orden "
                        + pago.getIdOrdenPaypal() + " en PayPal");
                return;
            }

            // Idempotency key derivada del id del pago (igual que
            // "retiro-" + idSolicitud en los payouts): un reintento sobre el
            // mismo pago no genera un segundo reembolso del lado de PayPal.
            payPalClient.llamarPayPalIdempotente(
                    "/v2/payments/captures/" + idCaptura + "/refund",
                    HttpMethod.POST, objectMapper.createObjectNode(),
                    "reembolso-" + pago.getIdPago());

            pago.setEstadoFondos(FONDOS_REEMBOLSADO);
            pago.setMensajeError(null);
            transaccionPagoRepository.save(PaymentTransaction.builder()
                    .pago(pago).tipoTransaccion("Reembolso").monto(pago.getMontoRetenido()).build());
        } catch (HttpStatusCodeException e) {
            pago.setEstadoFondos(FONDOS_REEMBOLSO_FALLIDO);
            pago.setMensajeError("Error de PayPal (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            log.error("Error reembolsando el pago {}: {}", pago.getIdPago(), e.getResponseBodyAsString());
        } catch (Exception e) {
            pago.setEstadoFondos(FONDOS_REEMBOLSO_FALLIDO);
            pago.setMensajeError("Error al comunicarse con PayPal: " + e.getMessage());
            log.error("Error reembolsando el pago {}", pago.getIdPago(), e);
        }
    }

    /** Busca la captura COMPLETED de una orden; PayPal separa el id de orden del id de captura. */
    private String obtenerIdCaptura(String orderId) {
        JsonNode orden = payPalClient.llamarPayPal("/v2/checkout/orders/" + orderId, HttpMethod.GET, null);
        for (JsonNode unidad : orden.path("purchase_units")) {
            for (JsonNode captura : unidad.path("payments").path("captures")) {
                if ("COMPLETED".equals(captura.path("status").asText())) {
                    return captura.path("id").asText(null);
                }
            }
        }
        return null;
    }

    /**
     * Libera los fondos al creador sin pasar por PayPal (decisión de un
     * administrador de que el trabajo ya se realizó pese a la cancelación).
     * Misma tasa de comisión y mismo par de transacciones Egreso/Comisión que
     * DeliverableServiceImpl.aprobarEntrega.
     */
    private void ejecutarLiberacionPorCancelacion(EscrowPayment pago) {
        pago.setEstadoFondos(FONDOS_LIBERADO);
        pago.setMensajeError(null);

        BigDecimal comision = pago.getMontoRetenido().multiply(tasaComision);
        BigDecimal pagoCreador = pago.getMontoRetenido().subtract(comision);

        transaccionPagoRepository.save(PaymentTransaction.builder()
                .pago(pago).tipoTransaccion("Egreso").monto(pagoCreador).build());
        transaccionPagoRepository.save(PaymentTransaction.builder()
                .pago(pago).tipoTransaccion("Comision").monto(comision).build());
    }

    /** Mismo patrón que RevisionTicketServiceImpl.tienePermisoDeSoporteOAdmin. */
    private boolean tienePermisoAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

}
