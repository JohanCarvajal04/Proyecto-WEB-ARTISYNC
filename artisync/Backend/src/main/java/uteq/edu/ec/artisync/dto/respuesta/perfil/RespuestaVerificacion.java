package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record RespuestaVerificacion(
        Long idCertificado,
        Long idUsuario,
        String tipoDocumento,
        String nombreEstadoVerificacion,
        String veredictoIa,
        BigDecimal puntajeConfianzaIa,
        String razonIa,
        String datosExtraidosIa,
        LocalDateTime fechaDictamenIa,
        Long idModerador,
        LocalDateTime fechaDecision,
        String notaModerador,
        LocalDateTime fechaAnalisis
) {
}
