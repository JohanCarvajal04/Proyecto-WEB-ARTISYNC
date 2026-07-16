package uteq.edu.ec.artisync.dto.peticion.pedido;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionEtapaConfig {

    @NotBlank(message = "El nombre de la etapa es obligatorio")
    private String nombreEtapa;

    @NotNull(message = "El numero de orden es obligatorio")
    private Integer numeroOrden;

    private boolean esEtapaFinal;
}
