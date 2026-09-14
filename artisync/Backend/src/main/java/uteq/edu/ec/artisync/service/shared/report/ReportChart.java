package uteq.edu.ec.artisync.service.shared.report;

import java.util.Map;

/**
 * Representa una gráfica estadística calculada para incluir en reportes PDF o Excel.
 * @param titulo título de la gráfica
 * @param subtitulo subtítulo o aclaración de la gráfica
 * @param imagenPng imagen de la gráfica ya renderizada, en formato PNG
 * @param datos serie de datos representada (etiqueta a valor)
 */
public record ReportChart(
        String titulo,
        String subtitulo,
        byte[] imagenPng,
        Map<String, Long> datos
) {
}
