package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEntregable {

    private Long idEntregable;
    private Long idPedido;
    private String urlVersionMarcaAgua;
    private String urlVersionLimpia;
    private Boolean estaLiberado;
}
