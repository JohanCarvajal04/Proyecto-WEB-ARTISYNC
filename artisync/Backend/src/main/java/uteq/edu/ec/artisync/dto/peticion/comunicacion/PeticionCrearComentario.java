package uteq.edu.ec.artisync.dto.peticion.comunicacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO para agregar un comentario a un ítem de portafolio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearComentario {

    @NotBlank(message = "El texto del comentario es obligatorio")
    @Size(max = 2000, message = "El comentario no puede superar los 2000 caracteres")
    private String textoComentario;
}
