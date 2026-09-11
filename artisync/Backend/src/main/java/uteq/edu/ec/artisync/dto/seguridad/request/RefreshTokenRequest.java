package uteq.edu.ec.artisync.dto.seguridad.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload que contiene el token de refresco seguro para obtener un nuevo token JWT de acceso.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud opcional para refrescar el token de acceso mediante cuerpo JSON si no se utiliza cookie HttpOnly")
public class RefreshTokenRequest {

    @Schema(description = "Refresh Token emitido durante el inicio de sesiÃ³n", example = "eyJhbGciOiJIUzI1NiIsIn...")
    private String refreshToken;
}



