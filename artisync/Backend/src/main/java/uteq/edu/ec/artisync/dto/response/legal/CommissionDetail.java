package uteq.edu.ec.artisync.dto.response.legal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una fila del detalle de fn_reporte_comisiones_creador. */
public record CommissionDetail(Long idTransaccion, Long idPedido, String servicio, String tipo,
                               BigDecimal monto, LocalDateTime fechaEjecucion) {
}
