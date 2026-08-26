package uteq.edu.ec.artisync.service.shared.reporte;

/** Un documento ya renderizado, listo para devolverse como descarga. */
public record DocumentoGenerado(byte[] contenido, String contentType, String nombreArchivo) {
}
