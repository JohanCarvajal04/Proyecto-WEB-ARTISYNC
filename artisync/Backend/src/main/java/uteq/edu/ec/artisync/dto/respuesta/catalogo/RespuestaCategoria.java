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
public class RespuestaCategoria {

    private Long idCategoria;
    private String nombreCategoria;
    private Boolean estadoActiva;

    /** Flujo que heredan los pedidos de esta categoría; null si no se asignó. */
    private Long idFlujo;
    private String nombreFlujo;

    private LocalDateTime actualizadoEn;
}
