package uteq.edu.ec.artisync.dto.request.profile;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Declaracion de uso etico de IA por parte del creador.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 *
 * @param idUsuario id del usuario creador que declara el uso de IA
 * @param idEstadoVerificacion estado inicial de verificación del certificado
 * @param urlDocumentoS3 referencia de almacenamiento del documento subido
 * @param puntajeConfianzaIa puntaje de confianza (0.00 a 1.00) devuelto por el análisis de IA
 */
public record CreateAiCertificateRequest(
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



