package uteq.edu.ec.artisync.dto.respuesta.social;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un ganador de sorteo.
 * Solo visible cuando el estado del sorteo es 'Finalizado'.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaGanador {

    private Long idParticipacion;
    private Long idUsuario;
    private String nombreUsuario;
    private LocalDateTime fechaNotificacionPremio;
}
