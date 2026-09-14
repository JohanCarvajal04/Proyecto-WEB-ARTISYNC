package uteq.edu.ec.artisync.dto.response.legal;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fila del panel de supervisión de Pagos y Garantías (Escrow) del Auditor
 * Financiero.
 *
 * @param idPago id del pago en garantía
 * @param idContrato id del contrato asociado
 * @param idPedido id del pedido asociado
 * @param tituloServicio título del servicio contratado
 * @param idUsuarioCliente id del usuario cliente
 * @param nombreCliente nombre completo del cliente
 * @param idPerfilCreador id del perfil del creador
 * @param nombreCreador nombre completo del creador
 * @param idOrdenPaypal id de la orden de PayPal asociada
 * @param montoRetenido monto retenido en garantía
 * @param estadoFondos estado actual de los fondos
 * @param fechaFormalizacion fecha de formalización del contrato
 */
@Builder
public record EscrowPaymentResponse(
        Long idPago,
        Long idContrato,
        Long idPedido,
        String tituloServicio,
        Long idUsuarioCliente,
        String nombreCliente,
        Long idPerfilCreador,
        String nombreCreador,
        String idOrdenPaypal,
        BigDecimal montoRetenido,
        String estadoFondos,
        LocalDateTime fechaFormalizacion
) {
}
