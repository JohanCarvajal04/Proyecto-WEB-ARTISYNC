package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record RespuestaTransaccionPago(
        Long idTransaccion,
        String tipoTransaccion,
        BigDecimal monto,
        LocalDateTime fechaEjecucion
) {
}
