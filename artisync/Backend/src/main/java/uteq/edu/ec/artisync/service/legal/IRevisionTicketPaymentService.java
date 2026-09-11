package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.entity.pedido.RevisionTicket;

/** REQ-F-022b/c: pago del cargo adicional de un ticket de revisión que superó el límite del contrato. */
public interface IRevisionTicketPaymentService {

    /**
     * Crea (o reutiliza, si ya existe una pendiente) la orden de pago en PayPal
     * para el cargo adicional de un ticket. Nunca propaga una falla de PayPal:
     * el ticket debe quedar creado igual, con el pago en estado fallido para
     * reintentar después.
     *
     * @param ticket ticket ya persistido, con {@code costoAdicionalGenerado > 0}
     */
    void crearOrdenPago(RevisionTicket ticket);

    /**
     * URL de aprobación de PayPal pendiente de pago para un ticket, o
     * {@code null} si el ticket no generó cargo, ya está pagado, o no tiene
     * ninguna orden creada. Lectura pura: no llama a PayPal.
     *
     * @param idTicket id del ticket
     */
    String obtenerUrlPagoPendiente(Long idTicket);

    /**
     * Procesa un evento de webhook de PayPal cuyo id de orden no correspondía
     * a ningún EscrowPayment (ver PaymentServiceImpl.procesarWebhookPayPal).
     *
     * @param idOrdenPaypal id de la orden de PayPal
     * @param tipoEvento    event_type del webhook (CHECKOUT.ORDER.APPROVED / PAYMENT.CAPTURE.COMPLETED)
     * @return {@code true} si la orden correspondía a un pago de ticket de revisión (procesado o no), {@code false} si no era de este módulo
     */
    boolean procesarWebhookOrden(String idOrdenPaypal, String tipoEvento);
}
