package uteq.edu.ec.artisync.dto.peticion.respaldo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PeticionCambiarEstadoProgramacion {

    @NotNull(message = "El estado activo/inactivo es obligatorio")
    private Boolean activo;
}
