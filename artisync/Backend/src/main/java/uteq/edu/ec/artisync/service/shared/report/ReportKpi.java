package uteq.edu.ec.artisync.service.shared.report;

/**
 * Métrica o indicador clave (KPI) para la cabecera ejecutiva del reporte.
 */
public record ReportKpi(
        String etiqueta,
        String valor,
        String descripcion
) {
}
