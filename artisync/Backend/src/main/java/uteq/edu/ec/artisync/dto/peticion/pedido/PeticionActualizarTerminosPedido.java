package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ajuste de precio y/o fecha de entrega negociado por chat antes de firmar el
 * contrato. Ambos campos son opcionales: se actualiza solo lo que venga.
 * El servicio rechaza la petición si no llega ningún campo, o si el contrato
 * ya tiene alguna firma (ver PedidoServicioImpl#actualizarTerminos).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionActualizarTerminosPedido {

    @DecimalMin(value = "0.01", message = "El precio pactado debe ser mayor a 0")
    private BigDecimal precioPactado;

    private LocalDateTime fechaEntregaEstimada;
}
