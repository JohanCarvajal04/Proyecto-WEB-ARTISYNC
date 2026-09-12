package uteq.edu.ec.artisync.service.shared.reporte;

/**
 * Un documento ya renderizado, listo para devolverse como descarga.
 *
 * @param contenido bytes del documento generado (PDF, CSV o XLSX)
 * @param contentType MIME type con el que se debe servir la respuesta
 * @param nombreArchivo nombre con el que se ofrece la descarga al cliente
 */
public record GeneratedDocument(byte[] contenido, String contentType, String nombreArchivo) {
}
