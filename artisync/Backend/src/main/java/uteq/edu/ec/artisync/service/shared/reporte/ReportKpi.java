package uteq.edu.ec.artisync.service.shared.reporte;

/**
 * Métrica o indicador clave (KPI) para la cabecera ejecutiva del reporte.
 */
public record ReportKpi(
        String etiqueta,
        String valor,
        String descripcion
) {
}
