package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaPedidoResumido {

    private Long idPedido;
    private String tituloServicio;
    private String etapaActual;
    private BigDecimal precioPactado;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaEntregaEstimada;
    private String nombreCreador;
    private String nombreCliente;
}
