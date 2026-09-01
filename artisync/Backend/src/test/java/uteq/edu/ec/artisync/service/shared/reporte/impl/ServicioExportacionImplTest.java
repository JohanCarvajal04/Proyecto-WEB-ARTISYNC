package uteq.edu.ec.artisync.service.shared.reporte.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.GeneradorReporte;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.service.shared.reporte.ReporteDePrueba;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicioExportacionImplTest {

    private final ServicioExportacionImpl servicio = new ServicioExportacionImpl(
            List.of(new GeneradorCsv(), new GeneradorXlsx(), stubPdf()));

    private static GeneradorReporte stubPdf() {
        return new GeneradorReporte() {
            @Override
            public FormatoReporte formato() {
                return FormatoReporte.PDF;
            }

            @Override
            public <T> DocumentoGenerado generar(ModeloReporte<T> modelo) {
                return new DocumentoGenerado(new byte[]{1}, FormatoReporte.PDF.contentType(), null);
            }
        };
    }

    @Test
    @DisplayName("Construye el nombre de archivo como slug_yyyyMMdd_HHmm.ext")
    void exportar_ConstruyeNombreDeArchivo() {
        DocumentoGenerado documento = servicio.exportar(ReporteDePrueba.modeloBasico(), FormatoReporte.CSV);

        assertThat(documento.nombreArchivo()).matches("reporte_de_prueba_\\d{8}_\\d{4}\\.csv");
    }

    @Test
    @DisplayName("Lanza ExcepcionReglaNegocio (422) si las filas superan el tope del formato")
    void exportar_SuperaTopeDeFilas_LanzaExcepcion() {
        List<ReporteDePrueba> filas = IntStream.range(0, FormatoReporte.PDF.topeFilas() + 1)
                .mapToObj(i -> new ReporteDePrueba("Fila " + i, BigDecimal.ONE, LocalDateTime.now(), (long) i))
                .toList();
        ModeloReporte<ReporteDePrueba> modelo = ModeloReporte.<ReporteDePrueba>builder()
                .titulo("Reporte Grande")
                .columnas(List.of(ColumnaReporte.texto("Nombre", ReporteDePrueba::nombre)))
                .filas(filas)
                .generadoPor("admin")
                .build();

        assertThatThrownBy(() -> servicio.exportar(modelo, FormatoReporte.PDF))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining(String.valueOf(FormatoReporte.PDF.topeFilas()));
    }

    @Test
    @DisplayName("Cada formato tiene su propio tope: lo que excede a PDF no excede a CSV")
    void exportar_TopesIndependientesPorFormato() {
        List<ReporteDePrueba> filas = IntStream.range(0, FormatoReporte.PDF.topeFilas() + 1)
                .mapToObj(i -> new ReporteDePrueba("Fila " + i, BigDecimal.ONE, LocalDateTime.now(), (long) i))
                .toList();
        ModeloReporte<ReporteDePrueba> modelo = ModeloReporte.<ReporteDePrueba>builder()
                .titulo("Reporte Grande")
                .columnas(List.of(ColumnaReporte.texto("Nombre", ReporteDePrueba::nombre)))
                .filas(filas)
                .generadoPor("admin")
                .build();

        DocumentoGenerado documento = servicio.exportar(modelo, FormatoReporte.CSV);

        assertThat(documento.contenido()).isNotEmpty();
    }

    @Test
    @DisplayName("Falla al construirse si falta un generador para algún formato")
    void constructor_FaltaGenerador_Falla() {
        assertThatThrownBy(() -> new ServicioExportacionImpl(List.of(new GeneradorCsv())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Falla al construirse si hay dos generadores para el mismo formato")
    void constructor_GeneradorDuplicado_Falla() {
        assertThatThrownBy(() -> new ServicioExportacionImpl(
                List.of(new GeneradorCsv(), new GeneradorCsv(), new GeneradorXlsx(), stubPdf())))
                .isInstanceOf(IllegalStateException.class);
    }
}
