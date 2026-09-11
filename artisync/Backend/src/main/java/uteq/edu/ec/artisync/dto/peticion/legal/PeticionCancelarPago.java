package uteq.edu.ec.artisync.dto.peticion.legal;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** REQ-NF-019: cancelación de un pedido con fondos ya retenidos en escrow. */
@Data
public class PeticionCancelarPago {

    /** "REEMBOLSAR" (default si es null) o "LIBERAR" (solo administrador). */
    @Size(max = 20, message = "La acción sobre los fondos no puede superar los 20 caracteres")
    private String accionFondos;

    @Size(max = 500, message = "El motivo no puede superar los 500 caracteres")
    private String motivo;
}
