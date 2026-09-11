package uteq.edu.ec.artisync.service.legal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import uteq.edu.ec.artisync.entity.legal.PagoTicketRevision;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.TicketRevision;
import uteq.edu.ec.artisync.repository.legal.PagoTicketRevisionRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;
import uteq.edu.ec.artisync.service.legal.IPagoTicketRevisionServicio;
import uteq.edu.ec.artisync.service.shared.paypal.PayPalClient;

import java.math.BigDecimal;

/**
 * REQ-F-022b/c: cobro de la revisión adicional cuando un ticket de revisión
 * supera el límite del contrato. Deliberadamente autocontenido respecto a
 * PagoServicioImpl (no reutiliza sus métodos privados de captura/creación):
 * mismo motivo que ReconciliacionPayPalEjecutorServicio, no arriesgar el
 * flujo de escrow principal ya verificado a cambio de ahorrar ~15 líneas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PagoTicketRevisionServicioImpl implements IPagoTicketRevisionServicio {

    private static final String ESTADO_PENDIENTE = "Pendiente";
    private static final String ESTADO_PAGADO = "Pagado";

    private static final String EVENTO_ORDEN_APROBADA = "CHECKOUT.ORDER.APPROVED";

    private final PagoTicketRevisionRepository pagoTicketRevisionRepository;
    private final NotificacionService notificacionService;
    private final PayPalClient payPalClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    @Override
    @Transactional
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param ticket parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void crearOrdenPago(TicketRevision ticket) {
        try {
            Pedido pedido = ticket.getPedido();
            BigDecimal monto = ticket.getCostoAdicionalGenerado();

            JsonNode orden = crearOrdenEnPayPal(pedido.getIdPedido(), monto);
            String orderId = orden.path("id").asText();
            String approvalUrl = extraerApprovalUrl(orden);

            PagoTicketRevision pago = pagoTicketRevisionRepository.findByTicketIdTicket(ticket.getIdTicket())
                    .orElseGet(() -> PagoTicketRevision.builder().ticket(ticket).build());
            pago.setIdOrdenPaypal(orderId);
            pago.setUrlAprobacion(approvalUrl);
            pago.setMonto(monto);
            pago.setEstadoPago(ESTADO_PENDIENTE);
            pago.setMensajeError(null);
            pagoTicketRevisionRepository.save(pago);

            log.info("Orden PayPal {} creada para el cargo adicional del ticket de revision {}",
                    orderId, ticket.getIdTicket());
        } catch (Exception e) {
            // No debe impedir que el ticket quede creado: el cliente puede
            // reintentar el pago más tarde (limitación aceptada: hoy no hay un
            // mecanismo automático de reintento, ver docs/requisitos/SRS.md).
            log.error("Error al crear la orden de pago del ticket de revision {}: {}",
                    ticket.getIdTicket(), e.getMessage(), e);
        }
    }

    private JsonNode crearOrdenEnPayPal(Long idPedido, BigDecimal monto) {
        String retorno = frontendUrl + "/pedido/" + idPedido;

        ObjectNode raiz = objectMapper.createObjectNode();
        raiz.put("intent", "CAPTURE");

        ObjectNode unidad = raiz.putArray("purchase_units").addObject();
        unidad.put("description", "ARTISYNC - Revision adicional del pedido #" + idPedido);
        unidad.putObject("amount")
                .put("currency_code", "USD")
                .put("value", monto.toPlainString());

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

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idTicket identificador unico que referencia de manera univoca al registro
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public String obtenerUrlPagoPendiente(Long idTicket) {
        return pagoTicketRevisionRepository.findByTicketIdTicket(idTicket)
                .filter(pago -> ESTADO_PENDIENTE.equals(pago.getEstadoPago()))
                .map(PagoTicketRevision::getUrlAprobacion)
                .orElse(null);
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idOrdenPaypal identificador unico que referencia de manera univoca al registro
     * @param tipoEvento parametro requerido para la correcta ejecucion del procedimiento
     * @return valor logico verdadero si la comprobacion fue exitosa, o falso si no cumplio los requisitos
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public boolean procesarWebhookOrden(String idOrdenPaypal, String tipoEvento) {
        PagoTicketRevision pago = pagoTicketRevisionRepository.findByIdOrdenPaypal(idOrdenPaypal).orElse(null);
        if (pago == null) {
            return false;
        }

        // Misma idempotencia por estado que PagoServicioImpl.procesarWebhookPayPal:
        // un PAYMENT.CAPTURE.COMPLETED que llega despues de que CHECKOUT.ORDER.APPROVED
        // ya confirmo el pago se absorbe aqui como duplicado, sin volver a capturar.
        if (!ESTADO_PENDIENTE.equalsIgnoreCase(pago.getEstadoPago())) {
            log.info("Webhook PayPal duplicado para la orden {} del ticket de revision {}: el pago ya esta en {}",
                    idOrdenPaypal, pago.getTicket().getIdTicket(), pago.getEstadoPago());
            return true;
        }

        if (EVENTO_ORDEN_APROBADA.equals(tipoEvento) && !capturarOrden(idOrdenPaypal)) {
            log.error("No se pudo capturar la orden {} del ticket de revision {}; el pago sigue pendiente",
                    idOrdenPaypal, pago.getTicket().getIdTicket());
            return true;
        }

        pago.setEstadoPago(ESTADO_PAGADO);
        pago.setMensajeError(null);
        pagoTicketRevisionRepository.save(pago);

        TicketRevision ticket = pago.getTicket();
        Pedido pedido = ticket.getPedido();
        log.info("Pago del ticket de revision {} confirmado. Monto: ${}", ticket.getIdTicket(), pago.getMonto());

        String mensaje = "Se confirmó el pago de la revisión adicional del pedido \""
                + pedido.getServicio().getTituloServicio() + "\".";
        notificacionService.notificar(pedido.getUsuarioCliente(), "PAGO_CONFIRMADO", mensaje);
        notificacionService.notificar(pedido.getServicio().getPerfil().getUsuario(), "PAGO_CONFIRMADO", mensaje);
        return true;
    }

    /** Misma logica que PagoServicioImpl.capturarOrden, duplicada a proposito (ver Javadoc de la clase). */
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
}
