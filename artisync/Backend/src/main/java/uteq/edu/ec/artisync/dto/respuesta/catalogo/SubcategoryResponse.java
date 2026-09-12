package uteq.edu.ec.artisync.dto.respuesta.catalogo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Respuesta con los datos de una subcategoría del catálogo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryResponse implements Serializable {

    private Long idSubcategoria;
    private Long idCategoria;
    private String nombreCategoria;
    private String nombreSubcategoria;

    /** null = la creó un admin/moderador. */
    private Long idUsuarioCreador;
    private String nombreCreador;
    private Boolean revisado;

    private LocalDateTime actualizadoEn;
}
