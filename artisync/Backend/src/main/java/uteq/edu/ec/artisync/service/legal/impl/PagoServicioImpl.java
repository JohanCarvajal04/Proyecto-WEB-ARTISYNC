package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
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
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PagoServicioImpl implements IPagoServicio {

    private final PagoGarantiaRepository pagoGarantiaRepository;
    private final ContratoRepository contratoRepository;
    private final TransaccionPagoRepository transaccionPagoRepository;

    @Value("${paypal.client-id:sandbox_client_id}")
    private String paypalClientId;

    @Value("${paypal.client-secret:sandbox_client_secret}")
    private String paypalClientSecret;

    @Value("${paypal.mode:sandbox}")
    private String paypalMode;

    @Value("${paypal.webhook-id:webhook_id}")
    private String paypalWebhookId;

    private String getPayPalBaseUrl() {
        return "sandbox".equalsIgnoreCase(paypalMode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }

    @Override
    @Transactional
    public RespuestaPago crearOrdenPayPal(Long idPedido, BigDecimal monto) {
        Contrato contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No existe contrato para el pedido"));

        // Verificar que ambas firmas estén completas
        if (contrato.getHashFirmaCreador() == null || contrato.getHashFirmaCliente() == null) {
            throw new ExcepcionReglaNegocio("El contrato debe estar firmado por ambas partes antes de realizar el pago");
        }

        BigDecimal montoFinal = monto != null ? monto : contrato.getPedido().getPrecioPactado();

        try {
            // Obtener access token de PayPal
            String accessToken = obtenerAccessToken();

            // Crear orden PayPal
            RestTemplate restTemplate = new RestTemplate();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            String body = """
                {
                    "intent": "CAPTURE",
                    "purchase_units": [{
                        "amount": {
                            "currency_code": "USD",
                            "value": "%s"
                        },
                        "description": "ARTISYNC - Pedido #%d"
                    }],
                    "application_context": {
                        "return_url": "https://artisync.com/pago/exito",
                        "cancel_url": "https://artisync.com/pago/cancelado"
                    }
                }
                """.formatted(montoFinal.toString(), idPedido);

            HttpEntity<String> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                    getPayPalBaseUrl() + "/v2/checkout/orders",
                    HttpMethod.POST, request, Map.class);

            Map responseBody = response.getBody();
            String orderId = (String) responseBody.get("id");

            // Extraer approval URL
            String approvalUrl = "";
            List<Map<String, String>> links = (List<Map<String, String>>) responseBody.get("links");
            for (Map<String, String> link : links) {
                if ("approve".equals(link.get("rel"))) {
                    approvalUrl = link.get("href");
                    break;
                }
            }

            // Guardar pago en garantía
            PagoGarantia pago = PagoGarantia.builder()
                    .contrato(contrato)
                    .idOrdenPaypal(orderId)
                    .montoRetenido(montoFinal)
                    .estadoFondos("Pendiente")
                    .build();
            pago = pagoGarantiaRepository.save(pago);

            log.info("Orden PayPal {} creada para pedido {} por ${}", orderId, idPedido, montoFinal);

            return RespuestaPago.builder()
                    .idPago(pago.getIdPago())
                    .idContrato(contrato.getIdContrato())
                    .idOrdenPaypal(orderId)
                    .montoRetenido(montoFinal)
                    .estadoFondos("Pendiente")
                    .approvalUrl(approvalUrl)
                    .build();

        } catch (ExcepcionReglaNegocio | ExcepcionRecursoNoEncontrado e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al crear orden PayPal para pedido {}", idPedido, e);
            throw new ExcepcionReglaNegocio("Error al comunicarse con PayPal: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void procesarWebhookPayPal(String payload, String transmissionId, String transmissionTime,
                                       String transmissionSig, String certUrl, String authAlgo, String webhookId) {
        // TODO: Implementar verificación de firma del webhook PayPal (RNF-14)
        log.info("Webhook PayPal recibido - transmissionId: {}", transmissionId);

        try {
            // Parsear el payload para obtener el orderId
            // En producción se usaría un ObjectMapper completo
            if (payload.contains("CHECKOUT.ORDER.APPROVED") || payload.contains("PAYMENT.CAPTURE.COMPLETED")) {
                // Buscar orderId en el payload
                int orderIdStart = payload.indexOf("\"id\":\"") + 6;
                int orderIdEnd = payload.indexOf("\"", orderIdStart);
                if (orderIdStart > 5 && orderIdEnd > orderIdStart) {
                    String orderId = payload.substring(orderIdStart, orderIdEnd);

                    PagoGarantia pago = pagoGarantiaRepository.findByIdOrdenPaypal(orderId)
                            .orElse(null);

                    if (pago != null) {
                        pago.setEstadoFondos("Retenido");
                        pagoGarantiaRepository.save(pago);

                        // Registrar transacción de ingreso
                        TransaccionPago transaccion = TransaccionPago.builder()
                                .pago(pago)
                                .tipoTransaccion("Ingreso")
                                .monto(pago.getMontoRetenido())
                                .build();
                        transaccionPagoRepository.save(transaccion);

                        log.info("Pago {} confirmado. Fondos retenidos: ${}", pago.getIdPago(), pago.getMontoRetenido());

                        // TODO M6: Notificar a ambas partes
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error procesando webhook PayPal", e);
        }
    }

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

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private String obtenerAccessToken() {
        RestTemplate restTemplate = new RestTemplate();

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
