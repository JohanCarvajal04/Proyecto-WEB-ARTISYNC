package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.Size;

public record PeticionActualizarPortafolio(
        Boolean esPublico,

        java.util.Map<String, String> opcionesPersonalizacion
) {
}
