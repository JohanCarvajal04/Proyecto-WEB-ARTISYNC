package uteq.edu.ec.artisync.service.shared.reporte.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import uteq.edu.ec.artisync.service.legal.IPdfGenerationService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportColumn;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ValueFormatter;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.ReportGenerator;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;
import uteq.edu.ec.artisync.service.shared.reporte.ReportTotal;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * PDF vía plantilla Thymeleaf (mismo motor que {@code EmailService}) + el
 * {@link IPdfGenerationService} ya existente para contratos — no se añade un
 * segundo renderizador de PDF. Thymeleaf escapa con {@code th:text} sin que el
 * dominio tenga que llamar a {@code HtmlUtils.htmlEscape} a mano.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PdfGenerator implements ReportGenerator {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String RUTA_LOGO = "reportes/logo-artisync.png";

    private final TemplateEngine templateEngine;
    private final IPdfGenerationService pdfGeneracionServicio;

    @Override
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ReportFormat formato() {
        return ReportFormat.PDF;
    }

    @Override
    public <T> GeneratedDocument generar(ReportModel<T> modelo) {
        Context contexto = new Context();
        contexto.setVariable("titulo", modelo.getTitulo());
        contexto.setVariable("subtitulo", modelo.getSubtitulo());
        contexto.setVariable("filtrosAplicados", modelo.getFiltrosAplicados());
        contexto.setVariable("generadoPor", modelo.getGeneradoPor());
        contexto.setVariable("generadoEn", modelo.getGeneradoEn().format(FORMATO_FECHA_HORA));
        contexto.setVariable("logoDataUri", logoComoDataUri());

        List<ReportColumn<T>> columnas = modelo.getColumnas();
        contexto.setVariable("encabezados", columnas.stream().map(ReportColumn::encabezado).toList());

        List<List<String>> filas = new ArrayList<>(modelo.getFilas().size());
        for (T fila : modelo.getFilas()) {
            List<String> valores = new ArrayList<>(columnas.size());
            for (ReportColumn<T> columna : columnas) {
                valores.add(ValueFormatter.texto(columna.extractor().apply(fila), columna.tipo()));
            }
            filas.add(valores);
        }
        contexto.setVariable("filas", filas);

        List<TotalConTexto> totales = modelo.getTotales().stream()
                .map(t -> new TotalConTexto(t.etiqueta(), ValueFormatter.texto(t.valor(), t.tipo())))
                .toList();
        contexto.setVariable("totales", totales);

        List<GraficaConDataUri> graficas = modelo.getGraficas().stream()
                .filter(g -> g.imagenPng() != null && g.imagenPng().length > 0)
                .map(g -> new GraficaConDataUri(
                        g.titulo(),
                        g.subtitulo(),
                        "data:image/png;base64," + Base64.getEncoder().encodeToString(g.imagenPng())))
                .toList();
        contexto.setVariable("graficas", graficas);
        contexto.setVariable("kpis", modelo.getKpis());

        String html = templateEngine.process("reportes/tabla", contexto);
        byte[] pdf = pdfGeneracionServicio.generarPdfDesdeHtml(html);
        return new GeneratedDocument(pdf, formato().contentType(), null);
    }

    /** El logo se codifica como {@code data:} URI para que el candado anti-SSRF de
     *  {@link IPdfGenerationService} no tenga que abrir el esquema {@code classpath:}. */
    private String logoComoDataUri() {
        try (InputStream is = new ClassPathResource(RUTA_LOGO).getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.warn("No se pudo cargar el logo del reporte ({}), se omite del PDF", RUTA_LOGO, e);
            return null;
        }
    }

    /**
     * Vista de {@link ReportTotal} con el valor ya formateado a texto para la plantilla.
     *
     * @param etiqueta nombre del total a mostrar
     * @param valorTexto valor ya formateado como moneda/texto, listo para la plantilla
     */
    public record TotalConTexto(String etiqueta, String valorTexto) {
    }

    /**
     * Vista de gráfica estadística convertida a Data URI para incrustación directa en HTML/PDF.
     *
     * @param titulo título de la gráfica
     * @param subtitulo subtítulo o descripción breve de la gráfica
     * @param dataUri imagen de la gráfica codificada como Data URI (base64)
     */
    public record GraficaConDataUri(String titulo, String subtitulo, String dataUri) {
    }
}
