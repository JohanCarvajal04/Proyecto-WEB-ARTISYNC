package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaFlujoTrabajo {

    private Long idFlujo;
    private String nombreFlujo;
    private String descripcionFlujo;
    private List<RespuestaEtapaConfig> etapas;
}
