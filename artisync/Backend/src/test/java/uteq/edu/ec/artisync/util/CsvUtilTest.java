package uteq.edu.ec.artisync.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CsvUtilTest {

    @Test
    @DisplayName("Valores normales pasan sin cambios")
    void escapeCsv_valorNormal() {
        assertThat(CsvUtil.escapeCsv("Ana Pérez")).isEqualTo("Ana Pérez");
    }

    @Test
    @DisplayName("null se convierte en cadena vacía")
    void escapeCsv_nulo() {
        assertThat(CsvUtil.escapeCsv(null)).isEqualTo("");
    }

    @Test
    @DisplayName("Comas, comillas y saltos de línea se escapan entre comillas dobles")
    void escapeCsv_caracteresEspeciales() {
        assertThat(CsvUtil.escapeCsv("Ana, \"la\" jefa\ncon salto"))
                .isEqualTo("\"Ana, \"\"la\"\" jefa\ncon salto\"");
    }

    // CSV injection: un dato de usuario (título de servicio, nombre) que
    // empieza con estos caracteres es interpretado como fórmula por
    // Excel/LibreOffice al abrir el CSV exportado. Mitigación OWASP:
    // anteponer un apóstrofo fuerza a tratar la celda como texto.

    @Test
    @DisplayName("Neutraliza una fórmula que empieza con '=', y además escapa sus comillas/comas internas")
    void escapeCsv_neutralizaIgual() {
        assertThat(CsvUtil.escapeCsv("=HYPERLINK(\"http://evil.com\",\"x\")"))
                .isEqualTo("\"'=HYPERLINK(\"\"http://evil.com\"\",\"\"x\"\")\"");
    }

    @Test
    @DisplayName("Neutraliza una fórmula que empieza con '+'")
    void escapeCsv_neutralizaMas() {
        assertThat(CsvUtil.escapeCsv("+cmd|'/c calc'!A1")).isEqualTo("'+cmd|'/c calc'!A1");
    }

    @Test
    @DisplayName("Neutraliza una fórmula que empieza con '-'")
    void escapeCsv_neutralizaMenos() {
        assertThat(CsvUtil.escapeCsv("-2+3")).isEqualTo("'-2+3");
    }

    @Test
    @DisplayName("Neutraliza una fórmula que empieza con '@'")
    void escapeCsv_neutralizaArroba() {
        assertThat(CsvUtil.escapeCsv("@SUM(1,1)")).isEqualTo("\"'@SUM(1,1)\"");
    }

    @Test
    @DisplayName("Un dato que empieza con '=' y además contiene una coma queda escapado y neutralizado a la vez")
    void escapeCsv_formulaYComaALaVez() {
        assertThat(CsvUtil.escapeCsv("=A1,B1")).isEqualTo("\"'=A1,B1\"");
    }
}
