package uteq.edu.ec.artisync.service.shared.reporte;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Modelo de reporte independiente del formato de salida: un dominio (auditoría,
 * finanzas, contratos...) construye uno de estos y se lo entrega a
 * {@link IExportService}, que decide cómo se ve en CSV, XLSX o PDF.
 */
@Getter
@Builder
public class ReportModel<T> {
    private final String titulo;
    private final String subtitulo;
    /** Filtros aplicados por el usuario, para mostrarlos en la cabecera del documento
     *  (p. ej. "Desde" -> "2026-01-01"). No son datos del reporte, son su contexto. */
    @Builder.Default
    private final Map<String, String> filtrosAplicados = Map.of();
    private final List<ReportColumn<T>> columnas;
    private final List<T> filas;
    @Builder.Default
    private final List<ReportTotal> totales = List.of();
    @Builder.Default
    private final List<ReportChart> graficas = List.of();
    @Builder.Default
    private final List<ReportKpi> kpis = List.of();
    private final String generadoPor;
    @Builder.Default
    private final LocalDateTime generadoEn = LocalDateTime.now();
}
