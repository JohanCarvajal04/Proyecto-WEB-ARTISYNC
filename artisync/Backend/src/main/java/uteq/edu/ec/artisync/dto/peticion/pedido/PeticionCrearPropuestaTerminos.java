package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Propuesta de precio y/o fecha de entrega final, negociada por chat. Ambos
 * campos son opcionales: se propone solo lo que venga. El cambio no se aplica
 * al pedido hasta que la contraparte del proponente la acepte (ver
 * PedidoServicioImpl#proponerTerminos / #aceptarPropuestaTerminos).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearPropuestaTerminos {

    @DecimalMin(value = "0.01", message = "El precio propuesto debe ser mayor a 0")
    private BigDecimal precioPropuesto;

    @Future(message = "La fecha de entrega debe ser una fecha futura")
    private LocalDateTime fechaEntregaPropuesta;
}
