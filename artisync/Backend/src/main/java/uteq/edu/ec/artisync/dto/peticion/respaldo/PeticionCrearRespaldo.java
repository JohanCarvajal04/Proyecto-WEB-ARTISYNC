package uteq.edu.ec.artisync.dto.peticion.respaldo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;

@Data
public class PeticionCrearRespaldo {

    @NotNull(message = "El tipo de respaldo es obligatorio")
    private TipoRespaldo tipoRespaldo;
}
