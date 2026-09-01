package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.Builder;

import java.math.BigDecimal;

/** Una fila del resumen agregado por estado de fondos (tarjetas del panel). */
@Builder
public record RespuestaResumenEscrow(
        String estadoFondos,
        long cantidad,
        BigDecimal montoTotal
) {
}
