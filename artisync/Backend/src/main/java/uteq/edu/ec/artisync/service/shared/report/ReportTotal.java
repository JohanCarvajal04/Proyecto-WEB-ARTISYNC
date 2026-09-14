package uteq.edu.ec.artisync.service.shared.report;

/** Una fila del pie de totales (p. ej. "Monto bruto" / 1234.56 / MONEDA). No aparece
 *  en el CSV (que se deja como dato puro); sí en XLSX y PDF.
 *  @param etiqueta nombre del total
 *  @param valor valor del total
 *  @param tipo tipo lógico del valor, que decide el formato de celda */
public record ReportTotal(String etiqueta, Object valor, ColumnType tipo) {
}
