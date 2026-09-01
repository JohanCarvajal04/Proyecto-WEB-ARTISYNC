package uteq.edu.ec.artisync.service.shared.reporte.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import uteq.edu.ec.artisync.service.legal.IPdfGeneracionServicio;
import uteq.edu.ec.artisync.service.legal.impl.PdfGeneracionServicioImpl;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.service.shared.reporte.ReporteDePrueba;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeneradorPdfTest {

    @Mock
    private IPdfGeneracionServicio pdfGeneracionServicio;

    @Captor
    private ArgumentCaptor<String> htmlCaptor;

    /** SpringTemplateEngine (no el TemplateEngine base): usa SpringStandardDialect,
     *  que evalúa expresiones con SpringEL. La producción se conecta igual, vía
     *  spring-boot-starter-thymeleaf; el TemplateEngine base usa OGNL, que no está
     *  en el classpath del proyecto. */
    private static TemplateEngine crearTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Test
    @DisplayName("Escapa un valor con HTML/script embebido en una celda (Thymeleaf th:text)")
    void generar_EscapaContenidoMaliciosoDeCelda() {
        when(pdfGeneracionServicio.generarPdfDesdeHtml(htmlCaptor.capture())).thenReturn(new byte[0]);
        GeneradorPdf generador = new GeneradorPdf(crearTemplateEngine(), pdfGeneracionServicio);

        ModeloReporte<ReporteDePrueba> modelo = ModeloReporte.<ReporteDePrueba>builder()
                .titulo("Reporte")
                .columnas(List.of(ColumnaReporte.texto("Nombre", ReporteDePrueba::nombre)))
                .filas(List.of(new ReporteDePrueba(
                        "<script>alert(1)</script><img src=\"http://169.254.169.254/\">",
                        null, LocalDateTime.now(), 1L)))
                .generadoPor("admin")
                .build();

        generador.generar(modelo);

        String html = htmlCaptor.getValue();
        assertThat(html).doesNotContain("<script>alert(1)</script>");
        assertThat(html).doesNotContain("<img src=\"http://169.254.169.254/\">");
        assertThat(html).contains("&lt;script&gt;");
    }

    @Test
    @DisplayName("Pasa los filtros aplicados y el título a la plantilla")
    void generar_IncluyeFiltrosYTitulo() {
        when(pdfGeneracionServicio.generarPdfDesdeHtml(htmlCaptor.capture())).thenReturn(new byte[0]);
        GeneradorPdf generador = new GeneradorPdf(crearTemplateEngine(), pdfGeneracionServicio);

        generador.generar(ReporteDePrueba.modeloBasico());

        String html = htmlCaptor.getValue();
        assertThat(html).contains("Reporte de Prueba");
        assertThat(html).contains("Desde");
        assertThat(html).contains("2026-01-01");
    }

    @Test
    @DisplayName("Content-Type es application/pdf")
    void generar_ContentTypeCorrecto() {
        when(pdfGeneracionServicio.generarPdfDesdeHtml(htmlCaptor.capture())).thenReturn(new byte[]{1, 2, 3});
        GeneradorPdf generador = new GeneradorPdf(crearTemplateEngine(), pdfGeneracionServicio);

        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());

        assertThat(documento.contentType()).isEqualTo("application/pdf");
    }

    @Test
    @DisplayName("Extremo a extremo con el renderizador real: produce un PDF válido y el logo data: no dispara SSRF")
    void generar_ExtremoAExtremoProduceDocumentoPdfValido() {
        GeneradorPdf generador = new GeneradorPdf(crearTemplateEngine(), new PdfGeneracionServicioImpl());

        ModeloReporte<ReporteDePrueba> modelo = ModeloReporte.<ReporteDePrueba>builder()
                .titulo("Reporte E2E")
                .columnas(List.of(ColumnaReporte.texto("Nombre", ReporteDePrueba::nombre)))
                .filas(List.of(new ReporteDePrueba(
                        "<img src=\"http://169.254.169.254/latest/meta-data/\">",
                        null, LocalDateTime.now(), 1L)))
                .generadoPor("admin")
                .build();

        DocumentoGenerado documento = generador.generar(modelo);

        assertThat(documento.contenido()).isNotEmpty();
        String cabecera = new String(documento.contenido(), 0, Math.min(5, documento.contenido().length),
                StandardCharsets.US_ASCII);
        assertThat(cabecera).isEqualTo("%PDF-");
    }
}
