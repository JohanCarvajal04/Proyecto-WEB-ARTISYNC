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
public class PeticionActualizarAtributo {

    @NotBlank(message = "El valor asignado es obligatorio")
    @Size(max = 255, message = "El valor asignado no puede superar los 255 caracteres")
    private String valorAsignado;
}
