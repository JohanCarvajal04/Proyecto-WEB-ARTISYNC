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
public class WinnerResponse {

    private Long idParticipacion;
    private Long idUsuario;
    private String nombreUsuario;
    private LocalDateTime fechaNotificacionPremio;

    /** Premio ganado. REQ-F-023 (V39): cada ganador queda ligado a un premio individual. */
    private Long idPremio;
    private String descripcionPremio;
}
