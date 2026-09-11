package uteq.edu.ec.artisync.dto.seguridad.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload administrativo para crear un usuario saltando el flujo de registro publico.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100, message = "Los nombres no pueden superar los 100 caracteres")
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100, message = "Los apellidos no pueden superar los 100 caracteres")
    private String apellidos;

    @NotBlank(message = "El correo electrÃ³nico es obligatorio")
    @Email(message = "El correo electrÃ³nico no tiene un formato vÃ¡lido")
    @Size(max = 150, message = "El correo no puede superar los 150 caracteres")
    private String correo;

    @NotBlank(message = "La contraseÃ±a es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseÃ±a debe tener entre 8 y 100 caracteres")
    private String contrasena;

    private LocalDate fechaNacimiento;

    private Long idPais;

    private List<String> roles;

    @Builder.Default
    private Boolean estadoCuenta = true;
}



