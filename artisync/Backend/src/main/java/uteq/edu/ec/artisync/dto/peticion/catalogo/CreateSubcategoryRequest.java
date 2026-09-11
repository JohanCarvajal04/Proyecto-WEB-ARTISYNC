package uteq.edu.ec.artisync.dto.peticion.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Objeto de transferencia de datos utilizado como carga útil de entrada (Request Payload).
 * 
 * Propósito: Payload para registrar una nueva subcategoria vinculada a una categoria.
 * 
 * Este contrato de entrada contiene reglas de validación (Jakarta Bean Validation) 
 * para asegurar la integridad estructural y de negocio de los datos recibidos 
 * por la API antes de ser delegados a la capa de servicios.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubcategoryRequest {

    @NotNull(message = "El ID de la categoria es obligatorio")
    private Long idCategoria;

    @NotBlank(message = "El nombre de la subcategoria es obligatorio")
    @Size(max = 100, message = "El nombre de la subcategoria no puede superar los 100 caracteres")
    private String nombreSubcategoria;
}



