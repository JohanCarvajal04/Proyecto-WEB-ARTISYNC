package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionResponderBriefing;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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

    /**
     * Obligatorio solo si el servicio tiene un cuestionario asignado
     * (Servicio.briefingPlantilla): debe traer una respuesta por cada
     * pregunta de esa plantilla. Se valida en PedidoServicioImpl.crearPedido,
     * no aquí, porque la obligatoriedad depende del servicio elegido.
     */
    @Valid
    private List<PeticionResponderBriefing.RespuestaItem> respuestasBriefing;
}
