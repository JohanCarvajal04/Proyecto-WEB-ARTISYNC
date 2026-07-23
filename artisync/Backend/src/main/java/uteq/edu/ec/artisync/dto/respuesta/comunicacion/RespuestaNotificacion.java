package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para el centro de notificaciones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaNotificacion {

    private Long idNotificacion;
    private String tipoEvento;
    private String mensaje;
    private Boolean estaLeida;
    private LocalDateTime fechaEmision;
}
