package uteq.edu.ec.artisync.service.shared.reporte;

import java.util.Map;

/**
 * Representa una gráfica estadística calculada para incluir en reportes PDF o Excel.
 */
public record ReportChart(
        String titulo,
        String subtitulo,
        byte[] imagenPng,
        Map<String, Long> datos
) {
}
