package uteq.edu.ec.artisync.dto.peticion.catalogo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearCategoria {

    @NotBlank(message = "El nombre de la categoria es obligatorio")
    @Size(max = 100, message = "El nombre de la categoria no puede superar los 100 caracteres")
    private String nombreCategoria;

    @Builder.Default
    private Boolean estadoActiva = true;
}
