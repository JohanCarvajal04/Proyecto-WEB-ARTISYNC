package uteq.edu.ec.artisync.dto.peticion.perfil;

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
 */
public record CreateAiCertificateRequest(
        @NotNull(message = "El ID del usuario es obligatorio")
        Long idUsuario,

        @NotNull(message = "El ID del estado de verificaciÃ³n es obligatorio")
        Long idEstadoVerificacion,

        @NotBlank(message = "La URL del documento S3 es obligatoria")
        @Size(max = 255, message = "La URL del documento no puede superar los 255 caracteres")
        String urlDocumentoS3,

        @DecimalMin(value = "0.00", message = "El puntaje de confianza no puede ser negativo")
        @DecimalMax(value = "1.00", message = "El puntaje de confianza no puede superar 1.00")
        BigDecimal puntajeConfianzaIa
) {
}



