package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEtapaConfig {

    private Long idFlujoEtapa;
    private Long idEtapa;
    private String nombreEtapa;
    private Integer numeroOrden;
    private Boolean esEtapaFinal;
}
