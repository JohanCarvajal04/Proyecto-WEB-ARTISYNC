package uteq.edu.ec.artisync.service.shared.reporte.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.service.shared.reporte.ReporteDePrueba;
import uteq.edu.ec.artisync.util.CsvUtil;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorCsvTest {

    private final GeneradorCsv generador = new GeneradorCsv();

    @Test
    @DisplayName("Antepone BOM UTF-8 para que Excel no rompa tildes")
    void generar_AntteponeBom() {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());
        String csv = new String(documento.contenido(), StandardCharsets.UTF_8);

        assertThat(csv).startsWith(CsvUtil.BOM_UTF8);
    }

    @Test
    @DisplayName("Escapa comas, comillas y saltos de línea")
    void generar_EscapaCaracteresEspeciales() {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());
        String csv = new String(documento.contenido(), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"Ana, \"\"la\"\" jefa\ncon salto\"");
    }

    @Test
    @DisplayName("La cabecera lleva los encabezados de columna declarados")
    void generar_CabeceraCorrecta() {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());
        String csv = new String(documento.contenido(), StandardCharsets.UTF_8);

        assertThat(csv).contains("Nombre,Monto,Fecha,Id");
    }

    @Test
    @DisplayName("No incluye la fila de totales — el CSV se queda como dato puro")
    void generar_NoIncluyeTotales() {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());
        String csv = new String(documento.contenido(), StandardCharsets.UTF_8);

        assertThat(csv).doesNotContain("Monto total");
    }

    @Test
    @DisplayName("Los montos se formatean con punto decimal, no con el locale del sistema")
    void generar_MontoConPuntoDecimal() {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());
        String csv = new String(documento.contenido(), StandardCharsets.UTF_8);

        assertThat(csv).contains("1234.50");
        assertThat(csv).doesNotContain("1234,50");
    }

    @Test
    @DisplayName("Content-Type es text/csv con UTF-8")
    void generar_ContentTypeCorrecto() {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());

        assertThat(documento.contentType()).isEqualTo("text/csv; charset=UTF-8");
    }
}
