package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Intercambia el numeroOrden de dos etapas de un mismo flujo en una sola
 * transacción — es lo que usa "mover etapa arriba/abajo" en vez de dos PUT
 * independientes, que dejaban una ventana donde una validación de colisión
 * de orden habría rechazado un swap legítimo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionSwapEtapas {

    @NotNull(message = "El id de la primera etapa es obligatorio")
    private Long idFlujoEtapaA;

    @NotNull(message = "El id de la segunda etapa es obligatorio")
    private Long idFlujoEtapaB;
}
