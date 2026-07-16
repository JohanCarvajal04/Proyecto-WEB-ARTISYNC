package uteq.edu.ec.artisync.dto.peticion.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionFirmarContrato {

    // No requiere campos adicionales: la identidad del firmante se obtiene del JWT
    // y el contrato se identifica por el path variable {id}
    private String observacion;
}
