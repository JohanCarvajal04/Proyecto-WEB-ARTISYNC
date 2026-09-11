package uteq.edu.ec.artisync.dto.seguridad.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload administrativo para bloquear o activar una cuenta de usuario.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEstadoRequest {

    @NotNull(message = "El estado de la cuenta es obligatorio")
    private Boolean estadoCuenta;
}



