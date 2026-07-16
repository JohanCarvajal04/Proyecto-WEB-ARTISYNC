package uteq.edu.ec.artisync.dto.peticion.pedido;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionAvanzarEtapa {

    private String observacion;
}
