package uteq.edu.ec.artisync.dto.respuesta.legal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Una fila del reporte de contratos (proyección JPQL sobre Contrato + Pedido). */
public record FilaReporteContrato(Long idContrato, Long idPedido, String servicio, String cliente, String creador,
                                   BigDecimal precioPactado, Integer limiteRevisiones,
                                   LocalDateTime fechaFormalizacion, Boolean firmadoCliente, Boolean firmadoCreador) {
}
