package uteq.edu.ec.artisync.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;
import uteq.edu.ec.artisync.entity.legal.PaymentTransaction;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.repository.legal.EscrowPaymentRepository;
import uteq.edu.ec.artisync.repository.legal.PaymentTransactionRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;
import uteq.edu.ec.artisync.service.shared.paypal.PayPalClient;

/**
 * Extraído de PayPalReconciliationScheduler para que REQUIRES_NEW funcione de
 * verdad (mismo motivo documentado en RaffleExecutorService: this.metodo()
 * dentro de la misma clase se salta el proxy de Spring AOP).
 *
 * No reutiliza los métodos privados de PaymentServiceImpl (capturarOrden,
 * firmaVerificada, etc.): se mantiene deliberadamente autocontenido para no
 * arriesgar la lógica de idempotencia del webhook, ya verificada (REQ-NF-014),
 * a costa de duplicar ~15 líneas de captura.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PayPalReconciliationExecutorService {

    private static final String FONDOS_PENDIENTE = "Pendiente";
    private static final String FONDOS_RETENIDO = "Retenido";

    private final EscrowPaymentRepository pagoGarantiaRepository;
    private final PaymentTransactionRepository transaccionPagoRepository;
    private final NotificationService notificacionService;
    private final PayPalClient payPalClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Consulta el estado real de una orden de PayPal en {@code Pendiente} y la
     * reconcilia: confirma el pago si ya está {@code COMPLETED}, captura y
     * confirma si está {@code APPROVED}, o deja constancia en el log si sigue
     * pendiente o fue {@code VOIDED}. Relee el pago con lock antes de actuar
     * por si el webhook ya lo confirmó mientras tanto.
     *
     * @param idPago identificador del pago de garantía a reconciliar
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditable(accion = "PAGO_RECONCILIAR", modulo = AuditModule.FINANZAS,
            correoActor = "'sistema:paypal'",
            entidad = "pagos_garantia", idEntidad = "#idPago")
    public void reconciliar(Long idPago) {
        // Relectura con lock: si el webhook confirmó el pago entre que el
        // scheduler lo leyó y esta transacción arrancó, gana el webhook y aquí
        // no hay nada que hacer.
        EscrowPayment pago = pagoGarantiaRepository.findByIdParaActualizar(idPago).orElse(null);
        if (pago == null || !FONDOS_PENDIENTE.equalsIgnoreCase(pago.getEstadoFondos())) {
            return;
        }

        JsonNode orden;
        try {
            orden = payPalClient.callPayPal(
                    "/v2/checkout/orders/" + pago.getIdOrdenPaypal(), HttpMethod.GET, null);
        } catch (Exception e) {
            log.error("[PayPalReconciliationExecutorService] Error consultando la orden {} en PayPal: {}",
                    pago.getIdOrdenPaypal(), e.getMessage());
            return;
        }

        String estado = orden.path("status").asText();
        switch (estado) {
            case "COMPLETED" -> confirmarPago(pago, "reconciliación: la orden ya estaba COMPLETED en PayPal");
            case "APPROVED" -> capturarYConfirmar(pago);
            case "VOIDED" -> log.warn(
                    "[PayPalReconciliationExecutorService] Orden {} (pago {}) VOIDED en PayPal; sigue Pendiente, "
                            + "el cliente deberá iniciar un nuevo intento de pago",
                    pago.getIdOrdenPaypal(), pago.getIdPago());
            default -> log.info(
                    "[PayPalReconciliationExecutorService] Orden {} (pago {}) sigue en estado {} en PayPal; "
                            + "se reintenta en el próximo ciclo",
                    pago.getIdOrdenPaypal(), pago.getIdPago(), estado);
        }
    }

    private void capturarYConfirmar(EscrowPayment pago) {
        try {
            JsonNode respuesta = payPalClient.callPayPal(
                    "/v2/checkout/orders/" + pago.getIdOrdenPaypal() + "/capture",
                    HttpMethod.POST, objectMapper.createObjectNode());
            String estadoCaptura = respuesta.path("status").asText();
            if ("COMPLETED".equals(estadoCaptura)) {
                confirmarPago(pago, "reconciliación: orden APPROVED capturada de forma proactiva");
            } else {
                log.error("[PayPalReconciliationExecutorService] Captura de la orden {} devolvió estado {}",
                        pago.getIdOrdenPaypal(), estadoCaptura);
            }
        } catch (HttpStatusCodeException e) {
            if (e.getResponseBodyAsString().contains("ORDER_ALREADY_CAPTURED")) {
                confirmarPago(pago, "reconciliación: la orden ya estaba capturada");
                return;
            }
            log.error("[PayPalReconciliationExecutorService] Error capturando la orden {}: {}",
                    pago.getIdOrdenPaypal(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("[PayPalReconciliationExecutorService] Error capturando la orden {}",
                    pago.getIdOrdenPaypal(), e);
        }
    }

    private void confirmarPago(EscrowPayment pago, String motivo) {
        pago.setEstadoFondos(FONDOS_RETENIDO);
        pagoGarantiaRepository.save(pago);

        transaccionPagoRepository.save(PaymentTransaction.builder()
                .pago(pago)
                .tipoTransaccion("Ingreso")
                .monto(pago.getMontoRetenido())
                .build());

        log.info("[PayPalReconciliationExecutorService] Pago {} confirmado por {}. Fondos retenidos: ${}",
                pago.getIdPago(), motivo, pago.getMontoRetenido());

        Order pedido = pago.getContrato().getPedido();
        String mensaje = "El pago de tu pedido \"" + pedido.getServicio().getTituloServicio()
                + "\" fue confirmado. Los fondos quedan en garantía hasta la aprobación de la entrega.";
        notificacionService.notify(pedido.getUsuarioCliente(), "PAGO_CONFIRMADO", mensaje);
        notificacionService.notify(pedido.getServicio().getPerfil().getUsuario(), "PAGO_CONFIRMADO",
                "Se confirmó el pago de garantía para el pedido \"" + pedido.getServicio().getTituloServicio() + "\".");
    }
}
