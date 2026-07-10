package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;
import java.time.LocalDateTime;

@Builder
public record RespuestaPortafolio(
        Long idPortafolio,
        Long idPerfil,
        LocalDateTime fechaCreacion,
        Integer totalVisitasAcumuladas,
        Boolean esPublico,
        String colorPlantilla
) {
}
