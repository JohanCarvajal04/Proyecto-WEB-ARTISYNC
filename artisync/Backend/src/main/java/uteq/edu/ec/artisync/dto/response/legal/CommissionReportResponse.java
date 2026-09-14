package uteq.edu.ec.artisync.dto.response.legal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Cabecera + detalle de fn_reporte_comisiones_creador, ya parseado del JSONB.
 * @param idPerfil id del perfil de creador reportado
 * @param fechaDesde inicio del rango de fechas del reporte
 * @param fechaHasta fin del rango de fechas del reporte
 * @param tasaComision tasa de comisión de la plataforma aplicada en el período
 * @param totalPedidos total de pedidos liquidados en el período
 * @param totalOperaciones total de transacciones de pago en el período
 * @param montoBruto monto bruto facturado en el período
 * @param comision monto total de comisión retenida por la plataforma
 * @param montoNeto monto neto recibido por el creador
 * @param detalle detalle de cada transacción del período
 */
public record CommissionReportResponse(Long idPerfil, LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                          BigDecimal tasaComision, long totalPedidos, long totalOperaciones,
                                          BigDecimal montoBruto, BigDecimal comision, BigDecimal montoNeto,
                                          List<CommissionDetail> detalle) {
}
