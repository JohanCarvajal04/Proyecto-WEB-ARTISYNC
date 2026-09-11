package uteq.edu.ec.artisync.dto.respuesta.catalogo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long idCategoria;
    private String nombreCategoria;
    private Boolean estadoActiva;

    /** null = la creó un admin/moderador. */
    private Long idUsuarioCreador;
    private String nombreCreador;
    private Boolean revisado;

    private LocalDateTime actualizadoEn;
}
