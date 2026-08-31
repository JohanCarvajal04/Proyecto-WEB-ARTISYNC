package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Ejercita el renderizador real de openhtmltopdf (a diferencia de
 * ContratoServicioImplTest, que mockea IPdfGeneracionServicio). openhtmltopdf
 * exige XHTML estrictamente bien formado; un `<meta>` o `<hr>` sin autocerrar
 * pasa cualquier test que mockee el renderizador pero rompe la descarga real
 * con un 500 (ver V30__fix_plantilla_contrato_xhtml.sql).
 */
class PdfGeneracionServicioImplTest {

    private final PdfGeneracionServicioImpl servicio = new PdfGeneracionServicioImpl();

    @Test
    void generarPdfDesdeHtml_generaBytesConHtmlValido() {
        String html = "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"/>"
                + "<title>t</title></head><body><p>hola</p></body></html>";

        byte[] pdf = servicio.generarPdfDesdeHtml(html);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void generarPdfDesdeHtml_lanzaConMetaSinAutocerrar() {
        // Regresión del bug real: exactamente el HTML que sembraba V13 antes del fix.
        String html = "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<title>t</title></head><body><p>hola</p></body></html>";

        assertThatCode(() -> servicio.generarPdfDesdeHtml(html))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void generarPdfDesdeHtml_lanzaConEntidadHtmlNombradaSinDeclarar() {
        // Regresión: HtmlUtils.htmlEscape(String) sin especificar codificación
        // convierte tildes/ñ en entidades HTML nombradas (&iacute;, &ntilde;...),
        // que el parser XML estricto de openhtmltopdf rechaza por no estar
        // declaradas (solo conoce las 5 entidades predefinidas de XML). Este es
        // el HTML que se generaba antes del fix en ContratoServicioImpl
        // (CODIFICACION_ESCAPE = "UTF-8" en HtmlUtils.htmlEscape(valor, "UTF-8")).
        String html = "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"/>"
                + "<title>t</title></head><body><p>fotograf&iacute;a</p></body></html>";

        assertThatCode(() -> servicio.generarPdfDesdeHtml(html))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void generarPdfDesdeHtml_generaBytesConTildesLiteralesUtf8() {
        // Mismo texto que arriba, pero como lo produce ahora
        // HtmlUtils.htmlEscape(valor, "UTF-8"): la tilde queda como caracter
        // UTF-8 literal, válido en XML sin necesitar una entidad declarada.
        String html = "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\"/>"
                + "<title>t</title></head><body><p>fotografía</p></body></html>";

        byte[] pdf = servicio.generarPdfDesdeHtml(html);

        assertThat(pdf).isNotEmpty();
    }

    @Test
    void generarPdfDesdeHtml_generaBytesConPlantillaDeContratoCompleta() {
        // Misma forma que ContratoServicioImpl#renderizarContratoCompleto tras el fix:
        // plantilla real (meta autocerrado) + pie de firmas con <hr/> autocerrado.
        String html = "<!DOCTYPE html><html lang=\"es\">"
                + "<head><meta charset=\"UTF-8\"/><title>Contrato de Prestación de Servicios</title></head>"
                + "<body>"
                + "<h1>Contrato de Prestación de Servicios Creativos</h1>"
                + "<p>Entre <strong>Ana</strong> (\"el Creador\") y <strong>Luis</strong> (\"el Cliente\").</p>"
                + "<hr/><div style='font-size:10px; color:#666;'>"
                + "<p>Firma Creador (SHA-256): abc123</p>"
                + "<p>Firma Cliente (SHA-256): def456</p>"
                + "</div>"
                + "</body></html>";

        byte[] pdf = servicio.generarPdfDesdeHtml(html);

        assertThat(pdf).isNotEmpty();
    }
}
