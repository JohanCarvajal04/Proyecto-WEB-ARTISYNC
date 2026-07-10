package uteq.edu.ec.artisync.dto.respuesta.catalogo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaAtributo {

    private Long idServicioAtributo;
    private Long idAtributo;
    private String nombreAtributo;
    private String tipoDato;
    private String valorAsignado;
    private LocalDateTime actualizadoEn;
}
