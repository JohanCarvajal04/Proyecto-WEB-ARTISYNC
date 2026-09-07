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

    /** Opcional: si se omite, ReporteFinancieroServicioImpl usa `plataforma.comision-tasa`. */
    private BigDecimal tasaComision;
}
