package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEstadoSeguimiento {
    private Boolean esSeguidor;
    private Long totalSeguidores;
    private Boolean esPropioPerfil;
}
