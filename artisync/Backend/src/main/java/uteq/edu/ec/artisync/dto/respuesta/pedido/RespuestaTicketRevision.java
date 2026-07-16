package uteq.edu.ec.artisync.dto.respuesta.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaTicketRevision {

    private Long idTicket;
    private Long idPedido;
    private String descripcionMotivo;
    private String descripcionCliente;
    private String estadoTicket;
    private BigDecimal costoAdicionalGenerado;
    private String urlPagoAdicional;
}
