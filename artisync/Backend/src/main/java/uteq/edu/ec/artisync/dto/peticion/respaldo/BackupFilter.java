package uteq.edu.ec.artisync.dto.peticion.respaldo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uteq.edu.ec.artisync.dto.peticion.comun.FiltroRangoFechas;
import uteq.edu.ec.artisync.entity.respaldo.BackupStatus;
import uteq.edu.ec.artisync.entity.respaldo.BackupOrigin;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;

/**
 * Filtros de la pantalla de respaldos, ligados con @ModelAttribute. Todos son
 * opcionales.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BackupFilter extends FiltroRangoFechas {

    private BackupType tipoRespaldo;
    private BackupStatus estadoRespaldo;
    private BackupOrigin origen;
}
