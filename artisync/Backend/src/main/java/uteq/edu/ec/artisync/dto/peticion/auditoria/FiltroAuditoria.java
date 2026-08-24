package uteq.edu.ec.artisync.dto.peticion.auditoria;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Filtros de la pantalla de auditoría, ligados con @ModelAttribute. Todos son
 * opcionales.
 */
@Data
public class FiltroAuditoria {

    private String correoActor;
    private String accion;
    private String modulo;
    private String resultado;
    private String entidad;
    private Long idEntidad;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime desde;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime hasta;
}
