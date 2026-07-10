package uteq.edu.ec.artisync.dto.peticion.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearSubcategoria {

    @NotNull(message = "El ID de la categoria es obligatorio")
    private Long idCategoria;

    @NotBlank(message = "El nombre de la subcategoria es obligatorio")
    @Size(max = 100, message = "El nombre de la subcategoria no puede superar los 100 caracteres")
    private String nombreSubcategoria;
}
