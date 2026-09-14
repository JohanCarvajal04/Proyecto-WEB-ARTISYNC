package uteq.edu.ec.artisync.service.shared.report;

/**
 * Métrica o indicador clave (KPI) para la cabecera ejecutiva del reporte.
 * @param etiqueta nombre del indicador
 * @param valor valor del indicador, ya formateado como texto
 * @param descripcion aclaración adicional sobre el indicador
 */
public record ReportKpi(
        String etiqueta,
        String valor,
        String descripcion
) {
}
