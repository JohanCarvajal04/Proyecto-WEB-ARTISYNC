package uteq.edu.ec.artisync.dto.peticion.perfil;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Metadatos de la obra. El archivo viaja aparte como MultipartFile, porque el
 * cuerpo es multipart y no JSON.
 */
public record PeticionCrearPortafolioItem(
        @NotBlank(message = "El titulo de la obra es obligatorio")
        @Size(max = 150, message = "El titulo de la obra no puede superar los 150 caracteres")
        String tituloObra,

        String descripcionObra
) {
}
