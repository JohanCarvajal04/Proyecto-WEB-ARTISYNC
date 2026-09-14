package uteq.edu.ec.artisync.service.shared.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Único sitio de formateo de fecha/moneda del motor de reportes. Fija
 * {@link Locale} y {@link ZoneId} explícitos en vez de heredar los del sistema: el
 * exportador de transacciones original ({@code AuditServiceImpl.exportarTransaccionesCreadorCsv},
 * hoy retirado) usaba {@code String.format("%.2f", monto)}, que en una JVM con
 * locale es-ES emite "1234,56" — y parte la columna de un CSV separado por comas.
 * Fijar Locale.US aquí cierra ese bug de raíz para los tres formatos a la vez.
 */
public final class ValueFormatter {

    private static final Locale LOCALE = Locale.US;
    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");

    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", LOCALE);
    private static final DateTimeFormatter FORMATO_FECHA_HORA =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss", LOCALE);

    private ValueFormatter() {
    }

    /** @return la zona horaria fija que usa el motor de reportes ({@code America/Guayaquil}) */
    public static ZoneId zona() {
        return ZONA;
    }

    /**
     * Formatea un valor de columna según su {@link ColumnType}, con el
     * {@link Locale} y el {@link ZoneId} fijos de esta clase.
     * @param valor valor de la celda a formatear; {@code null} se representa como cadena vacía
     * @param tipo tipo de columna, que determina el patrón de formateo a aplicar
     * @return la representación textual del valor, lista para el documento exportado
     */
    public static String texto(Object valor, ColumnType tipo) {
        if (valor == null) {
            return "";
        }
        return switch (tipo) {
            case FECHA -> valor instanceof LocalDate fecha ? fecha.format(FORMATO_FECHA) : valor.toString();
            case FECHA_HORA ->
                    valor instanceof LocalDateTime fechaHora ? fechaHora.format(FORMATO_FECHA_HORA) : valor.toString();
            case MONEDA, DECIMAL -> moneda(valor).toPlainString();
            case BOOLEANO -> Boolean.TRUE.equals(valor) ? "Sí" : "No";
            default -> valor.toString();
        };
    }

    /**
     * Normaliza un valor numérico a dos decimales, redondeando hacia arriba en el punto medio.
     * @param valor valor numérico o su representación textual
     * @return el valor normalizado a escala 2
     */
    public static BigDecimal moneda(Object valor) {
        BigDecimal decimal = valor instanceof BigDecimal bd ? bd : new BigDecimal(valor.toString());
        return decimal.setScale(2, RoundingMode.HALF_UP);
    }
}
