package uteq.edu.ec.artisync.dto.peticion.pedido;

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

    private BigDecimal precioOfrecido;

    @Future(message = "La fecha de entrega debe ser una fecha futura")
    private LocalDateTime fechaEntregaEstimada;
}
