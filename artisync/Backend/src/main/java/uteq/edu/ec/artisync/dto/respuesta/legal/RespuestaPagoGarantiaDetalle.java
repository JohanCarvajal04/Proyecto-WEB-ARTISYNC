package uteq.edu.ec.artisync.dto.respuesta.legal;

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
 */
@Builder
public record RespuestaPagoGarantiaDetalle(
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
        List<RespuestaTransaccionPago> transacciones
) {
}



