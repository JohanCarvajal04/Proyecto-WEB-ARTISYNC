package uteq.edu.ec.artisync.service.shared.reporte;

import java.util.function.Function;

/**
 * Declara una columna de un {@link ReportModel}: qué encabezado lleva, de qué tipo
 * lógico es (decide el formato de celda) y cómo se extrae el valor de cada fila.
 * El dominio solo declara columnas; el {@link ReportGenerator} decide cómo se
 * renderizan en cada formato.
 */
public record ReportColumn<T>(String encabezado, ColumnType tipo, Function<T, Object> extractor,
                                 int anchoCaracteres) {

    private static final int ANCHO_POR_DEFECTO = 20;

    public static <T> ReportColumn<T> texto(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.TEXTO, extractor, ANCHO_POR_DEFECTO);
    }

    public static <T> ReportColumn<T> entero(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.ENTERO, extractor, 12);
    }

    public static <T> ReportColumn<T> decimal(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.DECIMAL, extractor, 14);
    }

    public static <T> ReportColumn<T> moneda(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.MONEDA, extractor, 16);
    }

    public static <T> ReportColumn<T> fecha(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.FECHA, extractor, 14);
    }

    public static <T> ReportColumn<T> fechaHora(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.FECHA_HORA, extractor, 20);
    }

    public static <T> ReportColumn<T> booleano(String encabezado, Function<T, Object> extractor) {
        return new ReportColumn<>(encabezado, ColumnType.BOOLEANO, extractor, 10);
    }

    public static <T> ReportColumn<T> texto(String encabezado, Function<T, Object> extractor, int anchoCaracteres) {
        return new ReportColumn<>(encabezado, ColumnType.TEXTO, extractor, anchoCaracteres);
    }
}
