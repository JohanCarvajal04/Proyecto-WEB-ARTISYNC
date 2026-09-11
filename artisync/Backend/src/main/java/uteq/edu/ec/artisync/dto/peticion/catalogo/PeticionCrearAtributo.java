package uteq.edu.ec.artisync.dto.peticion.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload para la creacion de un nuevo atributo dinamico personalizable.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearAtributo {

    @NotBlank(message = "El nombre del atributo es obligatorio")
    @Size(max = 100, message = "El nombre del atributo no puede superar los 100 caracteres")
    private String nombreAtributo;

    @NotBlank(message = "El valor asignado es obligatorio")
    @Size(max = 255, message = "El valor asignado no puede superar los 255 caracteres")
    private String valorAsignado;

    @NotBlank(message = "El tipo de dato es obligatorio")
    @Size(max = 50, message = "El tipo de dato no puede superar los 50 caracteres")
    private String tipoDato;
}



