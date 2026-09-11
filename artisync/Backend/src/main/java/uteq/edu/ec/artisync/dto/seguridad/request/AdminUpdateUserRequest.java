package uteq.edu.ec.artisync.dto.seguridad.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload administrativo para forzar la actualizacion de datos de cualquier usuario.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateUserRequest {

    @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
    private String nombres;

    @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
    private String apellidos;

    private LocalDate fechaNacimiento;

    private Long idPais;

    private Boolean estadoCuenta;

    private List<String> roles;

    private Boolean dosFactoresHabilitado;
}



