package uteq.edu.ec.artisync.service.shared.reporte;

/** Una fila del pie de totales (p. ej. "Monto bruto" / 1234.56 / MONEDA). No aparece
 *  en el CSV (que se deja como dato puro); sí en XLSX y PDF. */
public record ReportTotal(String etiqueta, Object valor, ColumnType tipo) {
}
