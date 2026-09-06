package uteq.edu.ec.artisync.dto.respuesta.perfil;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record RespuestaDatosPago(
        String correoPaypal,
        LocalDateTime fechaActualizacion
) {
}
