package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionAvanzarEtapa {

    @Size(max = 2000, message = "La observación no puede superar los 2000 caracteres")
    private String observacion;
}
