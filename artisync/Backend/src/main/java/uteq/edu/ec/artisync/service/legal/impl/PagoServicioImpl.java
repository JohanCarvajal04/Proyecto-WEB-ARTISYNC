package uteq.edu.ec.artisync.service.legal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPago;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;
import uteq.edu.ec.artisync.entity.legal.TransaccionPago;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.legal.PagoGarantiaRepository;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;
import uteq.edu.ec.artisync.service.legal.IPagoServicio;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServicioImpl implements IPagoServicio {

    /** Estados de `pagos_garantia.estado_fondos`. */
    private static final String FONDOS_PENDIENTE = "Pendiente";
    private static final String FONDOS_RETENIDO = "Retenido";

    private static final String EVENTO_ORDEN_APROBADA = "CHECKOUT.ORDER.APPROVED";
    private static final String EVENTO_CAPTURA_COMPLETADA = "PAYMENT.CAPTURE.COMPLETED";

    private final PagoGarantiaRepository pagoGarantiaRepository;
    private final ContratoRepository contratoRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;

    /**
     * Mapper propio, no el del contexto: la app no expone un bean ObjectMapper
     * (otros componentes ya lo inyectan con required=false), y además el
     * contrato con PayPal no debe verse afectado por personalizaciones de
     * serialización de la aplicación.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * No es `final` para que las pruebas puedan sustituirlo: el camino de
     * verificación y captura es el que mueve dinero y conviene poder ejercitarlo
     * sin salir a la red.
     */
    private RestTemplate restTemplate = new RestTemplate();

    @Value("${paypal.client-id:sandbox_client_id}")
    private String paypalClientId;

    @Value("${paypal.client-secret:sandbox_client_secret}")
    private String paypalClientSecret;

    @Value("${paypal.mode:sandbox}")
    private String paypalMode;

    @Value("${paypal.webhook-id:}")
    private String paypalWebhookId;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    private String getPayPalBaseUrl() {
        return "sandbox".equalsIgnoreCase(paypalMode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }

    // ── Creación de la orden ─────────────────────────────────────────────────

    @Override
    @Transactional
    @Auditable(accion = "PAGO_ORDEN_CREAR", modulo = ModuloAuditoria.FINANZAS,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{monto: #monto}")
    public RespuestaPago crearOrdenPayPal(Long idPedido, BigDecimal monto) {
        Contrato contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No existe contrato para el pedido"));

        if (contrato.getHashFirmaCreador() == null || contrato.getHashFirmaCliente() == null) {
            throw new ExcepcionReglaNegocio("El contrato debe estar firmado por ambas partes antes de realizar el pago");
        }

        Optional<PagoGarantia> existente = pagoGarantiaRepository.findByContratoIdContrato(contrato.getIdContrato());

        // Un pago ya confirmado no se vuelve a cobrar.
        if (existente.isPresent() && !FONDOS_PENDIENTE.equalsIgnoreCase(existente.get().getEstadoFondos())) {
            throw new ExcepcionReglaNegocio(
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
            PagoGarantia pago = existente.orElseGet(() -> PagoGarantia.builder().contrato(contrato).build());
            pago.setIdOrdenPaypal(orderId);
            pago.setMontoRetenido(montoFinal);
            pago.setEstadoFondos(FONDOS_PENDIENTE);
            pago = pagoGarantiaRepository.save(pago);

            log.info("Orden PayPal {} creada para pedido {} por ${}", orderId, idPedido, montoFinal);

            return RespuestaPago.builder()
                    .idPago(pago.getIdPago())
                    .idContrato(contrato.getIdContrato())
                    .idOrdenPaypal(orderId)
                    .montoRetenido(montoFinal)
                    .estadoFondos(FONDOS_PENDIENTE)
                    .approvalUrl(approvalUrl)
                    .build();

        } catch (ExcepcionReglaNegocio | ExcepcionRecursoNoEncontrado e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al crear orden PayPal para pedido {}", idPedido, e);
            throw new ExcepcionReglaNegocio("Error al comunicarse con PayPal: " + e.getMessage());
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
        unidad.put("description", "ARTISYNC - Pedido #" + idPedido);
        unidad.putObject("amount")
                .put("currency_code", "USD")
                .put("value", montoFinal.toPlainString());

        raiz.putObject("application_context")
                .put("return_url", retorno)
                .put("cancel_url", retorno);

        return llamarPayPal("/v2/checkout/orders", HttpMethod.POST, raiz);
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
    @Auditable(accion = "PAGO_WEBHOOK_RECIBIR", modulo = ModuloAuditoria.FINANZAS,
            correoActor = "'sistema:paypal'",
            detalle = "{transmissionId: #transmissionId}")
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

        PagoGarantia pago = pagoGarantiaRepository.findByIdOrdenPaypal(orderId).orElse(null);
        if (pago == null) {
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

        transaccionPagoRepository.save(TransaccionPago.builder()
                .pago(pago)
                .tipoTransaccion("Ingreso")
                .monto(pago.getMontoRetenido())
                .build());

        log.info("Pago {} confirmado y capturado. Fondos retenidos: ${}",
                pago.getIdPago(), pago.getMontoRetenido());

        // TODO M6: Notificar a ambas partes
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

            JsonNode respuesta = llamarPayPal("/v1/notifications/verify-webhook-signature",
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
            JsonNode respuesta = llamarPayPal("/v2/checkout/orders/" + orderId + "/capture",
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
    public RespuestaPago obtenerEstadoPago(Long idPedido) {
        Contrato contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No existe contrato para el pedido"));

        PagoGarantia pago = pagoGarantiaRepository.findByContratoIdContrato(contrato.getIdContrato())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No existe pago registrado para este pedido"));

        return RespuestaPago.builder()
                .idPago(pago.getIdPago())
                .idContrato(contrato.getIdContrato())
                .idOrdenPaypal(pago.getIdOrdenPaypal())
                .montoRetenido(pago.getMontoRetenido())
                .estadoFondos(pago.getEstadoFondos())
                .build();
    }

    // ── Cliente HTTP de PayPal ───────────────────────────────────────────────

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
}
