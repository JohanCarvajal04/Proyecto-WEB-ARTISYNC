package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Informacion sobre el recibo y trazabilidad de un pago externo.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
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



