package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.*;

/**
 * Estado de "me gusta" de un ítem de portafolio: total público y, si hay
 * usuario autenticado, si ya le dio like.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEstadoLike {

    private Long idItemPortafolio;
    private long totalLikes;
    private boolean meGusta;
}
