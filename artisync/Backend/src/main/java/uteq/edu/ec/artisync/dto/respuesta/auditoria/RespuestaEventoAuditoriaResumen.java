package uteq.edu.ec.artisync.dto.respuesta.auditoria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Fila de la tabla de auditoría. Deliberadamente SIN detalleCambio: no tiene
 * sentido enviar kilobytes de JSON por cada una de las 20 filas de una
 * página; el detalle se pide aparte, por id, cuando el usuario lo abre.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEventoAuditoriaResumen {
    private Long idEventoAuditoria;
    private LocalDateTime fechaEvento;
    private Long idUsuarioActor;
    private String correoActor;
    private String moduloAuditoria;
    private String accionAuditoria;
    private String resultadoEvento;
    private String entidadAfectada;
    private Long idEntidadAfectada;
    private String direccionIp;
}
