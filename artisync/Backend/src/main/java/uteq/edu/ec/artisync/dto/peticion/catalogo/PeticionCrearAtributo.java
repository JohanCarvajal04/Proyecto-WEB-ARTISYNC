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
