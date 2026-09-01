package uteq.edu.ec.artisync.dto.peticion.auditoria;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uteq.edu.ec.artisync.dto.peticion.comun.FiltroRangoFechas;

/**
 * Filtros de la pantalla de auditoría, ligados con @ModelAttribute. Todos son
 * opcionales.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FiltroAuditoria extends FiltroRangoFechas {

    private String correoActor;
    private String accion;
    private String modulo;
    private String resultado;
    private String entidad;
    private Long idEntidad;
}
