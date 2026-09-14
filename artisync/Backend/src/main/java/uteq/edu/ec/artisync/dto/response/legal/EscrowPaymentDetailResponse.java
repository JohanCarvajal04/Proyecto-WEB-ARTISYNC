package uteq.edu.ec.artisync.dto.response.legal;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Detalle de los fondos retenidos en Escrow (garantia) para un pedido en curso.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 *
 * @param idPago id del pago en garantía
 * @param idContrato id del contrato asociado
 * @param idPedido id del pedido asociado
 * @param tituloServicio título del servicio contratado
 * @param idUsuarioCliente id del usuario cliente
 * @param nombreCliente nombre completo del cliente
 * @param correoCliente correo del cliente
 * @param idPerfilCreador id del perfil del creador
 * @param nombreCreador nombre completo del creador
 * @param idOrdenPaypal id de la orden de PayPal asociada
 * @param montoRetenido monto retenido en garantía
 * @param estadoFondos estado actual de los fondos
 * @param fechaFormalizacion fecha de formalización del contrato
 * @param transacciones historial de transacciones del pago
 */
@Builder
public record EscrowPaymentDetailResponse(
        Long idPago,
        Long idContrato,
        Long idPedido,
        String tituloServicio,
        Long idUsuarioCliente,
        String nombreCliente,
        String correoCliente,
        Long idPerfilCreador,
        String nombreCreador,
        String idOrdenPaypal,
        BigDecimal montoRetenido,
        String estadoFondos,
        LocalDateTime fechaFormalizacion,
        List<PaymentTransactionResponse> transacciones
) {
}



