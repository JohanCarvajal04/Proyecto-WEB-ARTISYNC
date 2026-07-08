package uteq.edu.ec.artisync.dto.seguridad.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangeEstadoRequest {

    @NotNull(message = "El estado de la cuenta es obligatorio")
    private Boolean estadoCuenta;
}
