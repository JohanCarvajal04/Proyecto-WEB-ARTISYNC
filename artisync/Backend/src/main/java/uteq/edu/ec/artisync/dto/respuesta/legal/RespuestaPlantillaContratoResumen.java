package uteq.edu.ec.artisync.dto.respuesta.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO liviano para que el creador elija una plantilla activa al crear/editar su servicio. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaPlantillaContratoResumen {

    private Long idPlantilla;
    private String nombrePlantilla;
}
