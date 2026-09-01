package uteq.edu.ec.artisync.dto.peticion.legal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import uteq.edu.ec.artisync.dto.peticion.comun.FiltroRangoFechas;

import java.math.BigDecimal;

/** Filtros del reporte de comisiones de un creador, ligados con @ModelAttribute. */
@Data
@EqualsAndHashCode(callSuper = true)
public class FiltroReporteFinanciero extends FiltroRangoFechas {

    private Long idPerfil;

    /** Opcional: si se omite, fn_reporte_comisiones_creador usa su default (10%). */
    private BigDecimal tasaComision;
}
