package uteq.edu.ec.artisync.dto.response.legal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila del detalle de fn_reporte_comisiones_creador.
 * @param idTransaccion id de la transacción de pago
 * @param idPedido id del pedido asociado
 * @param servicio título del servicio contratado
 * @param tipo tipo de transacción
 * @param monto monto de la transacción
 * @param fechaEjecucion fecha en que se ejecutó la transacción
 */
public record CommissionDetail(Long idTransaccion, Long idPedido, String servicio, String tipo,
                               BigDecimal monto, LocalDateTime fechaEjecucion) {
}
