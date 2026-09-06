package uteq.edu.ec.artisync.dto.peticion.legal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PeticionSolicitudRetiro {

    @NotNull(message = "El monto solicitado es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto solicitado debe ser mayor que cero")
    private BigDecimal montoSolicitado;
}
