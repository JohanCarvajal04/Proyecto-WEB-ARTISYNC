package uteq.edu.ec.artisync.service.shared.reporte.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.service.shared.reporte.ReportColumn;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.ReportGenerator;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;
import uteq.edu.ec.artisync.service.shared.reporte.ReporteDePrueba;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportServiceImplTest {

    private final ExportServiceImpl servicio = new ExportServiceImpl(
            List.of(new CsvGenerator(), new XlsxGenerator(), stubPdf()));

    private static ReportGenerator stubPdf() {
        return new ReportGenerator() {
            @Override
            public ReportFormat formato() {
                return ReportFormat.PDF;
            }

            @Override
            public <T> GeneratedDocument generar(ReportModel<T> modelo) {
                return new GeneratedDocument(new byte[]{1}, ReportFormat.PDF.contentType(), null);
            }
        };
    }

    @Test
    @DisplayName("Construye el nombre de archivo como slug_yyyyMMdd_HHmm.ext")
    void exportar_ConstruyeNombreDeArchivo() {
        GeneratedDocument documento = servicio.exportar(ReporteDePrueba.modeloBasico(), ReportFormat.CSV);

        assertThat(documento.nombreArchivo()).matches("reporte_de_prueba_\\d{8}_\\d{4}\\.csv");
    }

    @Test
    @DisplayName("Lanza BusinessRuleException (422) si las filas superan el tope del formato")
    void exportar_SuperaTopeDeFilas_LanzaExcepcion() {
        List<ReporteDePrueba> filas = IntStream.range(0, ReportFormat.PDF.topeFilas() + 1)
                .mapToObj(i -> new ReporteDePrueba("Fila " + i, BigDecimal.ONE, LocalDateTime.now(), (long) i))
                .toList();
        ReportModel<ReporteDePrueba> modelo = ReportModel.<ReporteDePrueba>builder()
                .titulo("Reporte Grande")
                .columnas(List.of(ReportColumn.texto("Nombre", ReporteDePrueba::nombre)))
                .filas(filas)
                .generadoPor("admin")
                .build();

        assertThatThrownBy(() -> servicio.exportar(modelo, ReportFormat.PDF))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(String.valueOf(ReportFormat.PDF.topeFilas()));
    }

    @Test
    @DisplayName("Cada formato tiene su propio tope: lo que excede a PDF no excede a CSV")
    void exportar_TopesIndependientesPorFormato() {
        List<ReporteDePrueba> filas = IntStream.range(0, ReportFormat.PDF.topeFilas() + 1)
                .mapToObj(i -> new ReporteDePrueba("Fila " + i, BigDecimal.ONE, LocalDateTime.now(), (long) i))
                .toList();
        ReportModel<ReporteDePrueba> modelo = ReportModel.<ReporteDePrueba>builder()
                .titulo("Reporte Grande")
                .columnas(List.of(ReportColumn.texto("Nombre", ReporteDePrueba::nombre)))
                .filas(filas)
                .generadoPor("admin")
                .build();

        GeneratedDocument documento = servicio.exportar(modelo, ReportFormat.CSV);

        assertThat(documento.contenido()).isNotEmpty();
    }

    @Test
    @DisplayName("Falla al construirse si falta un generador para algún formato")
    void constructor_FaltaGenerador_Falla() {
        assertThatThrownBy(() -> new ExportServiceImpl(List.of(new CsvGenerator())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Falla al construirse si hay dos generadores para el mismo formato")
    void constructor_GeneradorDuplicado_Falla() {
        assertThatThrownBy(() -> new ExportServiceImpl(
                List.of(new CsvGenerator(), new CsvGenerator(), new XlsxGenerator(), stubPdf())))
                .isInstanceOf(IllegalStateException.class);
    }
}
