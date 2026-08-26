package uteq.edu.ec.artisync.service.shared.reporte;

import java.util.function.Function;

/**
 * Declara una columna de un {@link ModeloReporte}: qué encabezado lleva, de qué tipo
 * lógico es (decide el formato de celda) y cómo se extrae el valor de cada fila.
 * El dominio solo declara columnas; el {@link GeneradorReporte} decide cómo se
 * renderizan en cada formato.
 */
public record ColumnaReporte<T>(String encabezado, TipoColumna tipo, Function<T, Object> extractor,
                                 int anchoCaracteres) {

    private static final int ANCHO_POR_DEFECTO = 20;

    public static <T> ColumnaReporte<T> texto(String encabezado, Function<T, Object> extractor) {
        return new ColumnaReporte<>(encabezado, TipoColumna.TEXTO, extractor, ANCHO_POR_DEFECTO);
    }

    public static <T> ColumnaReporte<T> entero(String encabezado, Function<T, Object> extractor) {
        return new ColumnaReporte<>(encabezado, TipoColumna.ENTERO, extractor, 12);
    }

    public static <T> ColumnaReporte<T> decimal(String encabezado, Function<T, Object> extractor) {
        return new ColumnaReporte<>(encabezado, TipoColumna.DECIMAL, extractor, 14);
    }

    public static <T> ColumnaReporte<T> moneda(String encabezado, Function<T, Object> extractor) {
        return new ColumnaReporte<>(encabezado, TipoColumna.MONEDA, extractor, 16);
    }

    public static <T> ColumnaReporte<T> fecha(String encabezado, Function<T, Object> extractor) {
        return new ColumnaReporte<>(encabezado, TipoColumna.FECHA, extractor, 14);
    }

    public static <T> ColumnaReporte<T> fechaHora(String encabezado, Function<T, Object> extractor) {
        return new ColumnaReporte<>(encabezado, TipoColumna.FECHA_HORA, extractor, 20);
    }

    public static <T> ColumnaReporte<T> booleano(String encabezado, Function<T, Object> extractor) {
        return new ColumnaReporte<>(encabezado, TipoColumna.BOOLEANO, extractor, 10);
    }

    public static <T> ColumnaReporte<T> texto(String encabezado, Function<T, Object> extractor, int anchoCaracteres) {
        return new ColumnaReporte<>(encabezado, TipoColumna.TEXTO, extractor, anchoCaracteres);
    }
}
