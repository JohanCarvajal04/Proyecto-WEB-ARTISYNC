package uteq.edu.ec.artisync.dto.response.legal;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una fila del reporte de contratos (proyección JPQL sobre Contract + Order).
 * @param idContrato id del contrato
 * @param idPedido id del pedido asociado
 * @param servicio título del servicio contratado
 * @param cliente nombre completo del cliente
 * @param creador nombre completo del creador
 * @param precioPactado precio pactado en el contrato
 * @param limiteRevisiones número de revisiones incluidas
 * @param fechaFormalizacion fecha de formalización del contrato
 * @param firmadoCliente si el cliente ya firmó el contrato
 * @param firmadoCreador si el creador ya firmó el contrato
 */
public record ContractReportRow(Long idContrato, Long idPedido, String servicio, String cliente, String creador,
                                   BigDecimal precioPactado, Integer limiteRevisiones,
                                   LocalDateTime fechaFormalizacion, Boolean firmadoCliente, Boolean firmadoCreador) {
}
