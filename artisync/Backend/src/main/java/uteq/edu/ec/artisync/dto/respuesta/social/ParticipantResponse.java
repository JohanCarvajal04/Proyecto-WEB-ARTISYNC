package uteq.edu.ec.artisync.dto.respuesta.social;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un participante de sorteo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantResponse {

    private Long idParticipacion;
    private Long idUsuario;
    private String nombreUsuario;
    private LocalDateTime fechaInscripcion;
    private Boolean esGanador;
}
