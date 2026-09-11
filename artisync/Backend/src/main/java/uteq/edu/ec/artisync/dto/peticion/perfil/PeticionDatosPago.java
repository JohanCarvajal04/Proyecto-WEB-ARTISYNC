package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload para configurar los metodos de cobro del creador (ej. cuenta bancaria, PayPal).
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
public class PeticionDatosPago {

    @NotBlank(message = "El correo de PayPal es obligatorio")
    @Email(message = "El correo de PayPal no tiene un formato vÃ¡lido")
    @Size(max = 150, message = "El correo de PayPal no puede superar los 150 caracteres")
    private String correoPaypal;
}



