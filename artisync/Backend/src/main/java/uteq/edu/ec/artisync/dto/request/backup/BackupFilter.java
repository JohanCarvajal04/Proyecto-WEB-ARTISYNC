package uteq.edu.ec.artisync.dto.request.backup;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uteq.edu.ec.artisync.dto.request.comun.FiltroRangoFechas;
import uteq.edu.ec.artisync.entity.backup.BackupStatus;
import uteq.edu.ec.artisync.entity.backup.BackupOrigin;
import uteq.edu.ec.artisync.entity.backup.BackupType;

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
