package uteq.edu.ec.artisync.service.legal;

public interface IPdfGeneracionServicio {

    byte[] generarPdfDesdeHtml(String html);
}
