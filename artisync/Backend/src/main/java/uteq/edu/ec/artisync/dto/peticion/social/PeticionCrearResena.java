package uteq.edu.ec.artisync.dto.peticion.social;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * DTO de petición para crear una reseña de servicio.
 * RF-09: Calificación de 1 a 5 estrellas, comentario opcional (máx 2000 chars).
 * Solo el cliente del pedido puede dejar reseña, y solo post-entrega.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearResena {

    @NotNull(message = "La calificación es obligatoria")
    @Min(value = 1, message = "La calificación mínima es 1 estrella")
    @Max(value = 5, message = "La calificación máxima es 5 estrellas")
    private Integer calificacionEstrellas;

    @Size(max = 2000, message = "El texto de la reseña no puede superar los 2000 caracteres")
    private String textoResena;
}
