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
public class PeticionCrearEtiqueta {

    @NotBlank(message = "El nombre de la etiqueta es obligatorio")
    @Size(max = 50, message = "El nombre de la etiqueta no puede superar los 50 caracteres")
    private String nombreEtiqueta;
}
