package uteq.edu.ec.artisync.service.shared.reporte.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import uteq.edu.ec.artisync.service.legal.IPdfGeneracionServicio;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormateadorValores;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.GeneradorReporte;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.service.shared.reporte.TotalReporte;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * PDF vía plantilla Thymeleaf (mismo motor que {@code EmailService}) + el
 * {@link IPdfGeneracionServicio} ya existente para contratos — no se añade un
 * segundo renderizador de PDF. Thymeleaf escapa con {@code th:text} sin que el
 * dominio tenga que llamar a {@code HtmlUtils.htmlEscape} a mano.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeneradorPdf implements GeneradorReporte {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String RUTA_LOGO = "reportes/logo-artisync.png";

    private final TemplateEngine templateEngine;
    private final IPdfGeneracionServicio pdfGeneracionServicio;

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.PDF;
    }

    @Override
    public <T> DocumentoGenerado generar(ModeloReporte<T> modelo) {
        Context contexto = new Context();
        contexto.setVariable("titulo", modelo.getTitulo());
        contexto.setVariable("subtitulo", modelo.getSubtitulo());
        contexto.setVariable("filtrosAplicados", modelo.getFiltrosAplicados());
        contexto.setVariable("generadoPor", modelo.getGeneradoPor());
        contexto.setVariable("generadoEn", modelo.getGeneradoEn().format(FORMATO_FECHA_HORA));
        contexto.setVariable("logoDataUri", logoComoDataUri());

        List<ColumnaReporte<T>> columnas = modelo.getColumnas();
        contexto.setVariable("encabezados", columnas.stream().map(ColumnaReporte::encabezado).toList());

        List<List<String>> filas = new ArrayList<>(modelo.getFilas().size());
        for (T fila : modelo.getFilas()) {
            List<String> valores = new ArrayList<>(columnas.size());
            for (ColumnaReporte<T> columna : columnas) {
                valores.add(FormateadorValores.texto(columna.extractor().apply(fila), columna.tipo()));
            }
            filas.add(valores);
        }
        contexto.setVariable("filas", filas);

        List<TotalConTexto> totales = modelo.getTotales().stream()
                .map(t -> new TotalConTexto(t.etiqueta(), FormateadorValores.texto(t.valor(), t.tipo())))
                .toList();
        contexto.setVariable("totales", totales);

        String html = templateEngine.process("reportes/tabla", contexto);
        byte[] pdf = pdfGeneracionServicio.generarPdfDesdeHtml(html);
        return new DocumentoGenerado(pdf, formato().contentType(), null);
    }

    /** El logo se codifica como {@code data:} URI para que el candado anti-SSRF de
     *  {@link IPdfGeneracionServicio} no tenga que abrir el esquema {@code classpath:}. */
    private String logoComoDataUri() {
        try (InputStream is = new ClassPathResource(RUTA_LOGO).getInputStream()) {
            byte[] bytes = is.readAllBytes();
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.warn("No se pudo cargar el logo del reporte ({}), se omite del PDF", RUTA_LOGO, e);
            return null;
        }
    }

    /** Vista de {@link TotalReporte} con el valor ya formateado a texto para la plantilla. */
    public record TotalConTexto(String etiqueta, String valorTexto) {
    }
}
