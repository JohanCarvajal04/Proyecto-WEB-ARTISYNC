package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaPago {

    private Long idPago;
    private Long idContrato;
    private String idOrdenPaypal;
    private BigDecimal montoRetenido;
    private String estadoFondos;
    private String approvalUrl;
}
