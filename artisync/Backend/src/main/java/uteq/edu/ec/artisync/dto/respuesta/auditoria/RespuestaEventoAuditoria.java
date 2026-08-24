package uteq.edu.ec.artisync.dto.respuesta.auditoria;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/** Vista completa de un evento, para el modal de detalle. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaEventoAuditoria {
    private Long idEventoAuditoria;
    private LocalDateTime fechaEvento;
    private Long idUsuarioActor;
    private String correoActor;
    private String moduloAuditoria;
    private String accionAuditoria;
    private String resultadoEvento;
    private String entidadAfectada;
    private Long idEntidadAfectada;
    private Map<String, Object> detalleCambio;
    private String mensajeError;
    private String direccionIp;
    private String agenteUsuario;
    private String metodoHttp;
    private String rutaSolicitud;
    private Integer duracionMs;
}
