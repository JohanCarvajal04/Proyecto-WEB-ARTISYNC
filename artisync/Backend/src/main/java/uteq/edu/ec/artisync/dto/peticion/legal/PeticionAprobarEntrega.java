package uteq.edu.ec.artisync.dto.peticion.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PeticionAprobarEntrega {

    private String comentarioAprobacion;
}
