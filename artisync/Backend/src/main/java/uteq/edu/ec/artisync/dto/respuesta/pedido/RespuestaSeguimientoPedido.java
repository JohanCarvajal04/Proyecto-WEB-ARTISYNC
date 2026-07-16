package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaSeguimientoPedido {

    private Long idPedido;
    private String tituloServicio;
    private String etapaActual;
    private Integer etapaActualOrden;
    private Integer totalEtapas;
    private Double porcentajeProgreso;
    private LocalDateTime fechaUltimaActualizacion;
    private List<RespuestaEtapaConfig> etapasDelFlujo;
    private List<RespuestaHistorialEstado> historial;
}
