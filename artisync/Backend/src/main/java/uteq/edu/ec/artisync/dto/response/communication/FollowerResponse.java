package uteq.edu.ec.artisync.dto.response.communication;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la relación de seguimiento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowerResponse {

    private Long idSeguimiento;
    private Long idUsuarioSeguidor;
    private String nombreSeguidor;
    private Long idPerfilCreador;
    private Boolean notificacionesActivas;
    private LocalDateTime fechaSeguimiento;
}
