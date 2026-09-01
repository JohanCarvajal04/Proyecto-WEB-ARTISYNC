package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
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
public class PeticionCrearPedido {

    @NotNull(message = "El ID del servicio es obligatorio")
    private Long idServicio;

    @DecimalMin(value = "0.01", message = "El precio ofrecido debe ser mayor a 0")
    private BigDecimal precioOfrecido;

    @Future(message = "La fecha de entrega debe ser una fecha futura")
    private LocalDateTime fechaEntregaEstimada;
}
