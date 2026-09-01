package uteq.edu.ec.artisync.dto.peticion.legal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uteq.edu.ec.artisync.dto.peticion.comun.FiltroRangoFechas;

/** Filtros del reporte de contratos, ligados con @ModelAttribute (mismo estilo que FiltroReporteFinanciero). */
@Data
@EqualsAndHashCode(callSuper = true)
public class FiltroReporteContrato extends FiltroRangoFechas {

    /** Filtra por el perfil del creador dueño del servicio contratado (servicio.perfil.idPerfil). */
    private Long idPerfilCreador;

    /** Si es true, solo contratos con ambas firmas; si es false, solo los pendientes; si se omite, todos. */
    private Boolean soloFirmados;
}
