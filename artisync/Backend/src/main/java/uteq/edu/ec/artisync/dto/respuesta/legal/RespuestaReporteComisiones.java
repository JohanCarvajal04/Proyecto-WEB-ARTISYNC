package uteq.edu.ec.artisync.dto.respuesta.legal;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Cabecera + detalle de fn_reporte_comisiones_creador, ya parseado del JSONB. */
public record RespuestaReporteComisiones(Long idPerfil, LocalDateTime fechaDesde, LocalDateTime fechaHasta,
                                          BigDecimal tasaComision, long totalPedidos, long totalOperaciones,
                                          BigDecimal montoBruto, BigDecimal comision, BigDecimal montoNeto,
                                          List<DetalleComision> detalle) {
}
