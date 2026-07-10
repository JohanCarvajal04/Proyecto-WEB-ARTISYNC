package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.Size;

public record PeticionActualizarPortafolio(
        Boolean esPublico,

        @Size(max = 20, message = "El color de plantilla no puede superar los 20 caracteres")
        String colorPlantilla
) {
}
