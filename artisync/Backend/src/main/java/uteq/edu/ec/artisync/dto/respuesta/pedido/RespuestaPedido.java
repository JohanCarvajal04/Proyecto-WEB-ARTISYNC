package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaPedido {

    private Long idPedido;
    private Long idServicio;
    private String tituloServicio;
    private Long idCliente;
    private String nombreCliente;
    private Long idCreador;
    private String nombreCreador;
    private String etapaActual;
    private BigDecimal precioPactado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaEntregaEstimada;
    private String nombreFlujo;
    private List<RespuestaHistorialEstado> historial;
}
