package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPago;

import java.math.BigDecimal;

public interface IPagoServicio {

    RespuestaPago crearOrdenPayPal(Long idPedido, BigDecimal monto);

    void procesarWebhookPayPal(String payload, String transmissionId, String transmissionTime,
                                String transmissionSig, String certUrl, String authAlgo, String webhookId);

    RespuestaPago obtenerEstadoPago(Long idPedido);
}
