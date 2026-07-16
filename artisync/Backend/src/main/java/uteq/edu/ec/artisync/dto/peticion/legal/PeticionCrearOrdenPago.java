package uteq.edu.ec.artisync.dto.peticion.legal;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionCrearOrdenPago {

    @NotNull(message = "El monto es obligatorio")
    private BigDecimal monto;

    private String descripcion;
}
