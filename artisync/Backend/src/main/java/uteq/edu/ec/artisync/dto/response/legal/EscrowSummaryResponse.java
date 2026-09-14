package uteq.edu.ec.artisync.dto.response.legal;

import lombok.Builder;

import java.math.BigDecimal;

/**
 * Una fila del resumen agregado por estado de fondos (tarjetas del panel).
 * @param estadoFondos estado de fondos agrupado
 * @param cantidad cantidad de pagos en ese estado
 * @param montoTotal suma de los montos retenidos en ese estado
 */
@Builder
public record EscrowSummaryResponse(
        String estadoFondos,
        long cantidad,
        BigDecimal montoTotal
) {
}
