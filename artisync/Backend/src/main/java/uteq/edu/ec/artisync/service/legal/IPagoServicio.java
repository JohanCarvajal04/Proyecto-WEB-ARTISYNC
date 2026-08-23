package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPago;

import java.math.BigDecimal;

public interface IPagoServicio {

    RespuestaPago crearOrdenPayPal(Long idPedido, Long idCliente, BigDecimal monto);

    /**
     * El último parámetro es la cabecera PAYPAL-AUTH-VERSION, no el webhook-id:
     * se llamaba `webhookId` pero el controlador pasa ahí `authVersion`, y el
     * id del webhook sale de la configuración, no de la petición.
     */
    void procesarWebhookPayPal(String payload, String transmissionId, String transmissionTime,
                                String transmissionSig, String certUrl, String authAlgo, String authVersion);

    RespuestaPago obtenerEstadoPago(Long idPedido, Long idUsuario);
}
