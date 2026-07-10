package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record RespuestaCertificadoIa(
        Long idCertificado,
        Long idPerfil,
        Long idEstadoVerificacion,
        String nombreEstadoVerificacion,
        String urlDocumentoS3,
        BigDecimal puntajeConfianzaIa,
        LocalDateTime fechaAnalisis
) {
}
