package uteq.edu.ec.artisync.service.shared.almacenamiento;

import java.util.Map;

/**
 * Traduce el content-type de un archivo subido a la extensión con la que se
 * persiste. Cubre lo que la plataforma acepta: imágenes de verificación, PDF de
 * contratos y entregables, y video de portafolio. Un tipo desconocido cae en
 * ".bin" en lugar de disfrazarse de imagen.
 */
public final class ExtensionesArchivo {

    private static final Map<String, String> POR_TIPO_MIME = Map.ofEntries(
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/jpg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/webp", ".webp"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/bmp", ".bmp"),
            Map.entry("image/svg+xml", ".svg"),
            Map.entry("application/pdf", ".pdf"),
            Map.entry("application/msword", ".doc"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx"),
            Map.entry("application/vnd.ms-excel", ".xls"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx"),
            Map.entry("text/plain", ".txt"),
            Map.entry("video/mp4", ".mp4"),
            Map.entry("video/webm", ".webm"),
            Map.entry("video/quicktime", ".mov"),
            Map.entry("audio/mpeg", ".mp3"),
            Map.entry("audio/wav", ".wav"));

    private static final String EXTENSION_DESCONOCIDA = ".bin";

    private ExtensionesArchivo() {
    }

    public static String desde(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return EXTENSION_DESCONOCIDA;
        }
        // El navegador puede enviar "image/jpeg; charset=..."; solo interesa el tipo.
        String tipo = contentType.split(";")[0].trim().toLowerCase();
        return POR_TIPO_MIME.getOrDefault(tipo, EXTENSION_DESCONOCIDA);
    }

    /**
     * Camino inverso, para responder una descarga con su Content-Type real.
     * No se deriva de POR_TIPO_MIME dando vuelta el mapa porque varias entradas
     * comparten extensión (image/jpeg e image/jpg dan ambas ".jpg") y el
     * resultado dependería del orden de iteración; aquí se fija el canónico.
     */
    private static final Map<String, String> POR_EXTENSION = Map.ofEntries(
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".png", "image/png"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".bmp", "image/bmp"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".pdf", "application/pdf"),
            Map.entry(".doc", "application/msword"),
            Map.entry(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry(".xls", "application/vnd.ms-excel"),
            Map.entry(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry(".txt", "text/plain"),
            Map.entry(".mp4", "video/mp4"),
            Map.entry(".webm", "video/webm"),
            Map.entry(".mov", "video/quicktime"),
            Map.entry(".mp3", "audio/mpeg"),
            Map.entry(".wav", "audio/wav"));

    private static final String TIPO_GENERICO = "application/octet-stream";

    /**
     * Content-Type deducido de la extensión de la referencia. Un tipo
     * desconocido cae en octet-stream, que hace que el navegador descargue el
     * archivo en vez de intentar interpretarlo.
     */
    public static String contentTypeDe(String referencia) {
        if (referencia == null) {
            return TIPO_GENERICO;
        }
        int punto = referencia.lastIndexOf('.');
        if (punto < 0) {
            return TIPO_GENERICO;
        }
        return POR_EXTENSION.getOrDefault(referencia.substring(punto).toLowerCase(), TIPO_GENERICO);
    }
}
