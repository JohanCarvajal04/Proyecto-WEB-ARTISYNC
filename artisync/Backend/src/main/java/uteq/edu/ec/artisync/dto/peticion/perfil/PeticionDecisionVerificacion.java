package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PeticionDecisionVerificacion(
        @NotNull(message = "El nuevo estado de verificación es obligatorio")
        Long idEstadoVerificacion,

        @Size(max = 500, message = "La nota no puede superar los 500 caracteres")
        String notaModerador
) {
}
