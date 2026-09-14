package uteq.edu.ec.artisync.dto.request.profile;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Metadatos de la obra. El archivo viaja aparte como MultipartFile, porque el
 * cuerpo es multipart y no JSON.
 *
 * @param tituloObra título de la obra
 * @param descripcionObra descripción de la obra
 */
public record CreatePortfolioItemRequest(
        @NotBlank(message = "El titulo de la obra es obligatorio")
        @Size(max = 150, message = "El titulo de la obra no puede superar los 150 caracteres")
        String tituloObra,

        @Size(max = 5000, message = "La descripción de la obra no puede superar los 5000 caracteres")
        String descripcionObra
) {
}
