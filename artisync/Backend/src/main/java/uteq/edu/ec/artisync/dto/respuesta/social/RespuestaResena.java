package uteq.edu.ec.artisync.dto.respuesta.social;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para una reseña de servicio.
 * RF-09: Calificación 1-5 estrellas más texto opcional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaResena {

    private Long idResena;
    private Integer calificacionEstrellas;
    private String textoResena;
    private LocalDateTime fechaResena;

    /** Nombre completo del cliente que dejó la reseña. */
    private String nombreCliente;

    /** Título del servicio reseñado. */
    private String tituloServicio;
}
