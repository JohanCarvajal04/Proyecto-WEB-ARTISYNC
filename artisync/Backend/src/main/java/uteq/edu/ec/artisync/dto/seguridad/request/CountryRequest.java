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
 * Propósito: Payload administrativo para la creacion o actualizacion de un pais en el diccionario base.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CountryRequest {

    @NotBlank(message = "El nombre del paÃ­s es obligatorio")
    @Size(max = 100, message = "El nombre del paÃ­s no puede superar los 100 caracteres")
    private String nombrePais;
}



