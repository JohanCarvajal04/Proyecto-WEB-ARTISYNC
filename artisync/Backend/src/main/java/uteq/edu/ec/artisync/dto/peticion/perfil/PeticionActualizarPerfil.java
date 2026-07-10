package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.Size;

public record PeticionActualizarPerfil(
        @Size(max = 500, message = "La biografía no puede superar los 500 caracteres")
        String biografia,

        @Size(max = 255, message = "La URL de red social no puede superar los 255 caracteres")
        String urlRedSocial
) {
}
