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
public class RespuestaPropuestaTerminos {

    private Long idPropuesta;
    private Long idPedido;
    private Long idUsuarioPropuso;
    private String nombrePropuso;
    private BigDecimal precioPropuesto;
    private LocalDateTime fechaEntregaPropuesta;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaResolucion;
}
