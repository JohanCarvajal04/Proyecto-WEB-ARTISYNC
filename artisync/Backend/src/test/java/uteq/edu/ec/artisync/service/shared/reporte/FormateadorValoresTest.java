package uteq.edu.ec.artisync.service.shared.reporte;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regresión del bug real que motivó este módulo: el exportador de transacciones
 * original ({@code AuditServiceImpl.exportarTransaccionesCreadorCsv}, hoy retirado)
 * usaba {@code String.format("%.2f", monto)}, que hereda el locale por defecto de la
 * JVM. En una JVM con locale es-ES eso produce "1234,56" — coma decimal — y parte la
 * columna de un CSV separado por comas. Este test corre explícitamente con locale
 * es-ES para probar que {@link FormateadorValores} no hereda ese comportamiento.
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
        String resultado = FormateadorValores.texto(new BigDecimal("1234.56"), TipoColumna.MONEDA);

        assertThat(resultado).isEqualTo("1234.56");
        assertThat(resultado).doesNotContain(",");
    }

    @Test
    @DisplayName("moneda() redondea a 2 decimales con HALF_UP independientemente del locale")
    void moneda_RedondeaADosDecimales() {
        BigDecimal resultado = FormateadorValores.moneda(new BigDecimal("999.999"));

        assertThat(resultado).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Un decimal formateado como texto también usa punto, no coma")
    void texto_Decimal_UsaPuntoDecimal() {
        String resultado = FormateadorValores.texto(new BigDecimal("42.5"), TipoColumna.DECIMAL);

        assertThat(resultado).isEqualTo("42.50");
    }
}
