package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Respuesta con el progreso de un pedido dentro de su flujo de trabajo. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponse {

    private Long idPedido;
    private String tituloServicio;
    private String etapaActual;
    private Integer etapaActualOrden;
    private Integer totalEtapas;
    private Double porcentajeProgreso;
    private LocalDateTime fechaUltimaActualizacion;
    private List<StageConfigResponse> etapasDelFlujo;
    private List<StatusHistoryResponse> historial;
    /** La etapa actual exige entregable (etapasDelFlujo) y el pedido todavia no tiene uno subido. */
    private boolean bloqueadoPorEntregable;
}
