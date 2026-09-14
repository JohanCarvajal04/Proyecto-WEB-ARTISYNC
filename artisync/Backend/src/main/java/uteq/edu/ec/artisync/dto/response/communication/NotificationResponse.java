package uteq.edu.ec.artisync.dto.response.communication;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para el centro de notificaciones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long idNotificacion;
    private String tipoEvento;
    private String mensaje;
    private Boolean estaLeida;
    private LocalDateTime fechaEmision;
}
