package uteq.edu.ec.artisync.dto.peticion.comun;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

/**
 * Rango de fechas opcional, ligado con @ModelAttribute. Base compartida por
 * los filtros de reportes/paneles administrativos que aceptan "desde"/"hasta"
 * (auditoría, pagos en garantía, reporte de contratos, reporte financiero),
 * antes repetidos de forma idéntica en cada uno.
 */
@Data
public class FiltroRangoFechas {

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime desde;

    @DateTimeFormat(iso = ISO.DATE_TIME)
    private LocalDateTime hasta;
}
