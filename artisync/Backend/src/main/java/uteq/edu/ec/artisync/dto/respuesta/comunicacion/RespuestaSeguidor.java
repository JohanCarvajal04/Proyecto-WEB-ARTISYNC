package uteq.edu.ec.artisync.dto.respuesta.comunicacion;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para la relación de seguimiento.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaSeguidor {

    private Long idSeguimiento;
    private Long idUsuarioSeguidor;
    private String nombreSeguidor;
    private Long idPerfilCreador;
    private Boolean notificacionesActivas;
    private LocalDateTime fechaSeguimiento;
}
