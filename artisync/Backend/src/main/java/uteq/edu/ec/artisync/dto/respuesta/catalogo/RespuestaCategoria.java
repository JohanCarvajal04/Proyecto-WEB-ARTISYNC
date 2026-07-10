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
    private LocalDateTime actualizadoEn;
}
