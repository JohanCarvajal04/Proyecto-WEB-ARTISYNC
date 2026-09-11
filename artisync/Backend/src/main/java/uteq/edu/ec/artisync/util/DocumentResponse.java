package uteq.edu.ec.artisync.util;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;

/**
 * Convierte un {@link GeneratedDocument} en la respuesta HTTP de descarga.
 * Siempre {@code attachment}, nunca {@code inline} — mismo criterio que
 * {@code DeliverableController.responderArchivo}: un reporte puede incluir datos de
 * usuario, y servirlo inline abriría la puerta a XSS almacenado si el navegador
 * llegara a interpretarlo en el dominio de la plataforma.
 */
public final class DocumentResponse {

    private DocumentResponse() {
    }

    public static ResponseEntity<byte[]> de(GeneratedDocument documento) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(documento.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(documento.nombreArchivo()).toString())
                .body(documento.contenido());
    }
}
