package uteq.edu.ec.artisync.dto.seguridad.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload para reestablecer la contrasena utilizando un token de recuperacion valido.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "El token de recuperaciÃ³n es obligatorio")
    private String token;

    @NotBlank(message = "La nueva contraseÃ±a es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseÃ±a debe tener entre 8 y 100 caracteres")
    private String nuevaContrasena;
}



