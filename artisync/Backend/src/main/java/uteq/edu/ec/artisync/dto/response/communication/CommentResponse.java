package uteq.edu.ec.artisync.dto.response.communication;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para comentarios en ítems de portafolio.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long idComentario;
    private Long idItemPortafolio;
    private Long idUsuarioAutor;
    private String nombreAutor;
    private String textoComentario;
    private String estadoModeracion;
    private LocalDateTime fechaPublicacion;
}
