package uteq.edu.ec.artisync.service.shared.report;

import java.util.function.Function;

/**
 * Declara una columna de un {@link ReportModel}: qué encabezado lleva, de qué tipo
 * lógico es (decide el formato de celda) y cómo se extrae el valor de cada fila.
 * El dominio solo declara columnas; el {@link ReportGenerator} decide cómo se
 * renderizan en cada formato.
 *
 * @param <T> tipo de fila del modelo de reporte del que se extrae el valor
 * @param encabezado encabezado de la columna
 * @param tipo tipo lógico de la columna, que decide el formato de celda
 * @param extractor función que obtiene el valor de la columna a partir de una fila
 * @param anchoCaracteres ancho sugerido de la columna, en caracteres (usado por XLSX)
 */
public record ReportColumn<T>(String encabezado, ColumnType tipo, Function<T, Object> extractor,
                                 int anchoCaracteres) {

    private static final int ANCHO_POR_DEFECTO = 20;

    /**
     * Crea una columna de tipo texto plano, con el ancho por defecto ({@value #ANCHO_POR_DEFECTO}
     * caracteres) usado por el resto de fábricas cuando no se pide un ancho específico.
     *
     * @param encabezado encabezado de la columna
     * @param extractor función que obtiene el valor de la columna a partir de una fila
     * @return la columna configurada como {@link ColumnType#TEXTO}
     */
    public static <T> ReportColumn<T> texto(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.TEXTO, extractor, ANCHO_POR_DEFECTO);
    }

    /**
     * Crea una columna de tipo entero, sin decimales.
     *
     * @param encabezado encabezado de la columna
     * @param extractor función que obtiene el valor de la columna a partir de una fila
     * @return la columna configurada como {@link ColumnType#ENTERO}
     */
    public static <T> ReportColumn<T> entero(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.ENTERO, extractor, 12);
    }

    /**
     * Crea una columna de tipo decimal, formateada con dos cifras después del punto.
     *
     * @param encabezado encabezado de la columna
     * @param extractor función que obtiene el valor de la columna a partir de una fila
     * @return la columna configurada como {@link ColumnType#DECIMAL}
     */
    public static <T> ReportColumn<T> decimal(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.DECIMAL, extractor, 14);
    }

    /**
     * Crea una columna de tipo monetario, formateada con símbolo de moneda y dos decimales.
     *
     * @param encabezado encabezado de la columna
     * @param extractor función que obtiene el valor de la columna a partir de una fila
     * @return la columna configurada como {@link ColumnType#MONEDA}
     */
    public static <T> ReportColumn<T> moneda(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.MONEDA, extractor, 16);
    }

    /**
     * Crea una columna de tipo fecha (sin hora), formateada como {@code yyyy-MM-dd}.
     *
     * @param encabezado encabezado de la columna
     * @param extractor función que obtiene el valor de la columna a partir de una fila
     * @return la columna configurada como {@link ColumnType#FECHA}
     */
    public static <T> ReportColumn<T> date(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.FECHA, extractor, 14);
    }

    /**
     * Crea una columna de tipo fecha y hora, formateada como {@code yyyy-MM-dd HH:mm:ss}.
     *
     * @param encabezado encabezado de la columna
     * @param extractor función que obtiene el valor de la columna a partir de una fila
     * @return la columna configurada como {@link ColumnType#FECHA_HORA}
     */
    public static <T> ReportColumn<T> dateTime(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.FECHA_HORA, extractor, 20);
    }

    /**
     * Crea una columna de tipo booleano, representada en el documento exportado como "Sí"/"No".
     *
     * @param encabezado encabezado de la columna
     * @param extractor función que obtiene el valor de la columna a partir de una fila
     * @return la columna configurada como {@link ColumnType#BOOLEANO}
     */
    public static <T> ReportColumn<T> booleano(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.BOOLEANO, extractor, 10);
    }

    /**
     * Crea una columna de tipo texto plano con un ancho de columna explícito, para los casos en
     * que el ancho por defecto ({@value #ANCHO_POR_DEFECTO} caracteres) no se ajusta al contenido
     * esperado (p. ej. un correo electrónico o una descripción larga).
     *
     * @param encabezado encabezado de la columna
     * @param extractor función que obtiene el valor de la columna a partir de una fila
     * @param anchoCaracteres ancho sugerido de la columna, en caracteres (usado por XLSX)
     * @return la columna configurada como {@link ColumnType#TEXTO} con el ancho indicado
     */
    public static <T> ReportColumn<T> texto(String encabezado, Function<T, Object> extractor, int anchoCaracteres) {
        return new ReportColumn<>(encabezado, ColumnType.TEXTO, extractor, anchoCaracteres);
    }
}
