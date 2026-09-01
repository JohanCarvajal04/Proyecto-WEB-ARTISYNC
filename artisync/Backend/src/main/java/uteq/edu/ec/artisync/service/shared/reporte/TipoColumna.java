package uteq.edu.ec.artisync.service.shared.reporte;

/** Tipo lógico de una columna de reporte: decide el formato de celda que aplica
 *  cada {@link GeneradorReporte} (patrón numérico en XLSX, alineación en PDF...). */
public enum TipoColumna {
    TEXTO,
    ENTERO,
    DECIMAL,
    MONEDA,
    FECHA,
    FECHA_HORA,
    BOOLEANO
}
