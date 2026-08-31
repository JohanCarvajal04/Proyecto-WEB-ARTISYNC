package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record RespuestaColaVerificacion(
        Long idCertificado,
        Long idUsuario,
        String nombreUsuario,
        String tipoDocumento,
        String nombreEstado,
        String veredictoIa,
        BigDecimal puntajeConfianzaIa,
        LocalDateTime fechaAnalisis
) {
}
