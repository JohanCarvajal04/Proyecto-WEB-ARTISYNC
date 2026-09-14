package uteq.edu.ec.artisync.service.shared.report;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Cubre cada fábrica estática de {@link ReportColumn}, una por {@link ColumnType}. */
class ReportColumnTest {

    @Test
    void texto_creaColumnaDeTipoTextoConAnchoPorDefecto() {
        ReportColumn<String> columna = ReportColumn.texto("Nombre", s -> s);

        assertThat(columna.tipo()).isEqualTo(ColumnType.TEXTO);
        assertThat(columna.anchoCaracteres()).isEqualTo(20);
    }

    @Test
    void texto_conAnchoExplicito_loRespeta() {
        ReportColumn<String> columna = ReportColumn.texto("Nombre", s -> s, 40);

        assertThat(columna.anchoCaracteres()).isEqualTo(40);
    }

    @Test
    void entero_creaColumnaDeTipoEntero() {
        ReportColumn<Integer> columna = ReportColumn.entero("Cantidad", i -> i);

        assertThat(columna.tipo()).isEqualTo(ColumnType.ENTERO);
        assertThat(columna.anchoCaracteres()).isEqualTo(12);
    }

    @Test
    void decimal_creaColumnaDeTipoDecimal() {
        ReportColumn<Double> columna = ReportColumn.decimal("Promedio", d -> d);

        assertThat(columna.tipo()).isEqualTo(ColumnType.DECIMAL);
        assertThat(columna.anchoCaracteres()).isEqualTo(14);
    }

    @Test
    void moneda_creaColumnaDeTipoMoneda() {
        ReportColumn<Double> columna = ReportColumn.moneda("Monto", d -> d);

        assertThat(columna.tipo()).isEqualTo(ColumnType.MONEDA);
        assertThat(columna.anchoCaracteres()).isEqualTo(16);
    }

    @Test
    void date_creaColumnaDeTipoFecha() {
        ReportColumn<String> columna = ReportColumn.date("Fecha", s -> s);

        assertThat(columna.tipo()).isEqualTo(ColumnType.FECHA);
        assertThat(columna.anchoCaracteres()).isEqualTo(14);
    }

    @Test
    void dateTime_creaColumnaDeTipoFechaHora() {
        ReportColumn<String> columna = ReportColumn.dateTime("Fecha y hora", s -> s);

        assertThat(columna.tipo()).isEqualTo(ColumnType.FECHA_HORA);
        assertThat(columna.anchoCaracteres()).isEqualTo(20);
    }

    @Test
    void booleano_creaColumnaDeTipoBooleano() {
        ReportColumn<Boolean> columna = ReportColumn.booleano("Activo", b -> b);

        assertThat(columna.tipo()).isEqualTo(ColumnType.BOOLEANO);
        assertThat(columna.anchoCaracteres()).isEqualTo(10);
    }
}
