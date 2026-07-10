package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record PeticionCrearCertificadoIa(
        @NotNull(message = "El ID del perfil es obligatorio")
        Long idPerfil,

        @NotNull(message = "El ID del estado de verificación es obligatorio")
        Long idEstadoVerificacion,

        @NotBlank(message = "La URL del documento S3 es obligatoria")
        String urlDocumentoS3,

        BigDecimal puntajeConfianzaIa
) {
}
