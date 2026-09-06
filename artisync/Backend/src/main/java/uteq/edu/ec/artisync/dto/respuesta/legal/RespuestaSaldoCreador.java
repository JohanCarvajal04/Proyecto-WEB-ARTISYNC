package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RespuestaSaldoCreador(
        BigDecimal saldoDisponible,
        BigDecimal montoMinimoRetiro,
        boolean tieneCorreoPaypalConfigurado,
        boolean tieneSolicitudPendiente
) {
}
