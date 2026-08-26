package uteq.edu.ec.artisync.dto.peticion.legal;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

/** Filtros del reporte de comisiones de un creador, ligados con @ModelAttribute. */
@Data
public class FiltroReporteFinanciero {

    private Long idPerfil;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime desde;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime hasta;

    /** Opcional: si se omite, fn_reporte_comisiones_creador usa su default (10%). */
    private BigDecimal tasaComision;
}
