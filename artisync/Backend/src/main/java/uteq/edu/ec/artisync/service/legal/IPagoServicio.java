package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPago;

import java.math.BigDecimal;

public interface IPagoServicio {

    /**
     * Crea una orden de pago en garantía (escrow) en PayPal para el contrato de un pedido.
     * Solo puede iniciarla el cliente, y solo cuando el contrato ya está firmado por ambas partes.
     *
     * @param idPedido  id del pedido a pagar
     * @param idCliente id del usuario que inicia el pago, debe ser el cliente del pedido
     * @param monto     monto a pagar
     * @return la orden de pago creada, con la URL de aprobación de PayPal
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el pedido no tiene contrato
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el solicitante no es el cliente del pedido, si el contrato no está firmado por ambas partes, si el pedido ya tiene un pago en curso, o si falla la comunicación con PayPal
     */
    RespuestaPago crearOrdenPayPal(Long idPedido, Long idCliente, BigDecimal monto);

    /**
     * Procesa una notificación webhook de PayPal sobre el estado de un pago (aprobación,
     * captura o liberación de fondos), verificando la firma de la petición.
     * El último parámetro es la cabecera PAYPAL-AUTH-VERSION, no el webhook-id:
     * se llamaba `webhookId` pero el controlador pasa ahí `authVersion`, y el
     * id del webhook sale de la configuración, no de la petición.
     *
     * @param payload          cuerpo crudo del evento recibido
     * @param transmissionId   cabecera PAYPAL-TRANSMISSION-ID
     * @param transmissionTime cabecera PAYPAL-TRANSMISSION-TIME
     * @param transmissionSig  cabecera PAYPAL-TRANSMISSION-SIG
     * @param certUrl          cabecera PAYPAL-CERT-URL
     * @param authAlgo         cabecera PAYPAL-AUTH-ALGO
     * @param authVersion      cabecera PAYPAL-AUTH-VERSION
     */
    void procesarWebhookPayPal(String payload, String transmissionId, String transmissionTime,
                                String transmissionSig, String certUrl, String authAlgo, String authVersion);

    /**
     * Obtiene el estado del pago asociado a un pedido.
     *
     * @param idPedido  id del pedido
     * @param idUsuario id del usuario que consulta, debe ser cliente o creador del pedido
     * @return el estado actual del pago
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el pedido no tiene contrato, o no tiene pago registrado
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el solicitante no tiene acceso al pago de este pedido
     */
    RespuestaPago obtenerEstadoPago(Long idPedido, Long idUsuario);
}
