package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PeticionCrearCertificadoIa(
        @NotNull(message = "El ID del usuario es obligatorio")
        Long idUsuario,

        @NotNull(message = "El ID del estado de verificación es obligatorio")
        Long idEstadoVerificacion,

        @NotBlank(message = "La URL del documento S3 es obligatoria")
        @Size(max = 255, message = "La URL del documento no puede superar los 255 caracteres")
        String urlDocumentoS3,

        @DecimalMin(value = "0.00", message = "El puntaje de confianza no puede ser negativo")
        @DecimalMax(value = "1.00", message = "El puntaje de confianza no puede superar 1.00")
        BigDecimal puntajeConfianzaIa
) {
}
