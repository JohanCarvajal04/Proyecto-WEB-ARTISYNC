package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uteq.edu.ec.artisync.service.legal.IPdfGeneracionServicio;

import java.io.ByteArrayOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGeneracionServicioImpl implements IPdfGeneracionServicio {

    @Override
    public byte[] generarPdfDesdeHtml(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            com.openhtmltopdf.pdfboxout.PdfRendererBuilder builder =
                    new com.openhtmltopdf.pdfboxout.PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();

            log.info("PDF generado exitosamente ({} bytes)", os.size());
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF desde HTML", e);
            throw new RuntimeException("Error al generar el documento PDF: " + e.getMessage(), e);
        }
    }
}
