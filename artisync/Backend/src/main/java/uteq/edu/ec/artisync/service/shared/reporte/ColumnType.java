package uteq.edu.ec.artisync.service.shared.reporte;

/** Tipo lógico de una columna de reporte: decide el formato de celda que aplica
 *  cada {@link ReportGenerator} (patrón numérico en XLSX, alineación en PDF...). */
public enum ColumnType {
    TEXTO,
    ENTERO,
    DECIMAL,
    MONEDA,
    FECHA,
    FECHA_HORA,
    BOOLEANO
}
