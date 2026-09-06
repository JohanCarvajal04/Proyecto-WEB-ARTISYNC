package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record RespuestaSolicitudRetiro(
        Long idSolicitud,
        Long idUsuarioCreador,
        String nombreCreador,
        BigDecimal montoSolicitado,
        String correoPaypalDestino,
        String estado,
        String idPayoutPaypal,
        String notaAdmin,
        String mensajeError,
        LocalDateTime fechaSolicitud,
        LocalDateTime fechaDecision,
        LocalDateTime fechaPago
) {
}
