package uteq.edu.ec.artisync.service.shared.report;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regresión del bug real que motivó este módulo: el exportador de transacciones
 * original ({@code AuditServiceImpl.exportarTransaccionesCreadorCsv}, hoy retirado)
 * usaba {@code String.format("%.2f", monto)}, que hereda el locale por defecto de la
 * JVM. En una JVM con locale es-ES eso produce "1234,56" — coma decimal — y parte la
 * columna de un CSV separado por comas. Este test corre explícitamente con locale
 * es-ES para probar que {@link ValueFormatter} no hereda ese comportamiento.
 */
class FormateadorValoresTest {

    private Locale localeOriginal;

    @BeforeEach
    void fijarLocaleEspanol() {
        localeOriginal = Locale.getDefault();
        Locale.setDefault(new Locale("es", "ES"));
    }

    @AfterEach
    void restaurarLocale() {
        Locale.setDefault(localeOriginal);
    }

    @Test
    @DisplayName("Con locale es-ES por defecto en la JVM, el monto sigue usando punto decimal")
    void texto_ConLocaleEspanol_UsaPuntoDecimal() {
        String resultado = ValueFormatter.texto(new BigDecimal("1234.56"), ColumnType.MONEDA);

        assertThat(resultado).isEqualTo("1234.56");
        assertThat(resultado).doesNotContain(",");
    }

    @Test
    @DisplayName("moneda() redondea a 2 decimales con HALF_UP independientemente del locale")
    void moneda_RedondeaADosDecimales() {
        BigDecimal resultado = ValueFormatter.moneda(new BigDecimal("999.999"));

        assertThat(resultado).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Un decimal formateado como texto también usa punto, no coma")
    void texto_Decimal_UsaPuntoDecimal() {
        String resultado = ValueFormatter.texto(new BigDecimal("42.5"), ColumnType.DECIMAL);

        assertThat(resultado).isEqualTo("42.50");
    }

    @Test
    void texto_valorNull_devuelveCadenaVacia() {
        assertThat(ValueFormatter.texto(null, ColumnType.TEXTO)).isEmpty();
    }

    @Test
    void texto_fecha_usaElPatronYyyyMmDd() {
        String resultado = ValueFormatter.texto(LocalDate.of(2026, 3, 5), ColumnType.FECHA);

        assertThat(resultado).isEqualTo("2026-03-05");
    }

    @Test
    void texto_fechaConTipoIncorrecto_caeAlToString() {
        String resultado = ValueFormatter.texto("no es una fecha", ColumnType.FECHA);

        assertThat(resultado).isEqualTo("no es una fecha");
    }

    @Test
    void texto_fechaHora_usaElPatronConHora() {
        String resultado = ValueFormatter.texto(LocalDateTime.of(2026, 3, 5, 14, 30, 0), ColumnType.FECHA_HORA);

        assertThat(resultado).isEqualTo("2026-03-05 14:30:00");
    }

    @Test
    void texto_booleanoVerdadero_devuelveSi() {
        assertThat(ValueFormatter.texto(Boolean.TRUE, ColumnType.BOOLEANO)).isEqualTo("Sí");
    }

    @Test
    void texto_booleanoFalso_devuelveNo() {
        assertThat(ValueFormatter.texto(Boolean.FALSE, ColumnType.BOOLEANO)).isEqualTo("No");
    }

    @Test
    void texto_entero_usaElToStringPorDefecto() {
        assertThat(ValueFormatter.texto(42, ColumnType.ENTERO)).isEqualTo("42");
    }

    @Test
    void zona_devuelveLaZonaHorariaFijaDeGuayaquil() {
        assertThat(ValueFormatter.zona()).isEqualTo(ZoneId.of("America/Guayaquil"));
    }

    @Test
    void moneda_aceptaTextoNumerico() {
        BigDecimal resultado = ValueFormatter.moneda("15.5");

        assertThat(resultado).isEqualByComparingTo("15.50");
    }
}
