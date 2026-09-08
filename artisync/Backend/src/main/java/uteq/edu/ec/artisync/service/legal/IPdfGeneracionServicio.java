package uteq.edu.ec.artisync.service.legal;

public interface IPdfGeneracionServicio {

    /**
     * Renderiza un documento HTML a PDF.
     *
     * @param html contenido HTML a renderizar
     * @return los bytes del PDF generado
     */
    byte[] generarPdfDesdeHtml(String html);
}
