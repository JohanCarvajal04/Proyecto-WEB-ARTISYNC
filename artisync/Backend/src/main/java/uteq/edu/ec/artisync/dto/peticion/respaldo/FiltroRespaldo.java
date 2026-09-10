package uteq.edu.ec.artisync.dto.peticion.respaldo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uteq.edu.ec.artisync.dto.peticion.comun.FiltroRangoFechas;
import uteq.edu.ec.artisync.entity.respaldo.EstadoRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.OrigenRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;

/**
 * Filtros de la pantalla de respaldos, ligados con @ModelAttribute. Todos son
 * opcionales.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FiltroRespaldo extends FiltroRangoFechas {

    private TipoRespaldo tipoRespaldo;
    private EstadoRespaldo estadoRespaldo;
    private OrigenRespaldo origen;
}
