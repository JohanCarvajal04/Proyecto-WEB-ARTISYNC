package uteq.edu.ec.artisync.service.legal.impl;

import com.openhtmltopdf.outputdevice.helper.ExternalResourceControlPriority;
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
            // Hallazgo SEC-01 (auditoria de seguridad): sin esto, cualquier URI que
            // aparezca en el HTML (una <img src>, un @import de CSS...) dispara una
            // peticion HTTP/lectura de archivo hecha por ESTE SERVIDOR al renderizar
            // el PDF. Como generarContratoHtml() interpola descripcion_servicio y
            // nombres de usuario dentro del HTML, un cliente podia escribir
            // <img src="http://169.254.169.254/..."> en la descripcion de su servicio
            // y usar el PDF del contrato como SSRF, o file:// para leer archivos
            // locales del servidor. La plantilla sembrada (V13__seed_plantilla_contrato.sql)
            // es HTML+CSS inline puro sin un solo recurso externo, asi que bloquear
            // TODA carga externa (predicado constante `false`) no rompe nada legitimo;
            // si algun dia una plantilla necesita imagenes, sera una decision explicita,
            // no un descuido.
            builder.useExternalResourceAccessControl(
                    (uri, type) -> false,
                    ExternalResourceControlPriority.RUN_BEFORE_RESOLVING_URI);
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
