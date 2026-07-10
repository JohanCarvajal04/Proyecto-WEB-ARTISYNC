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
public class RespuestaSubcategoria {

    private Long idSubcategoria;
    private Long idCategoria;
    private String nombreCategoria;
    private String nombreSubcategoria;
    private LocalDateTime actualizadoEn;
}
