package uteq.edu.ec.artisync.service.shared.imagen;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PreprocesadorImagenIaTest {

    private final PreprocesadorImagenIa preprocesador = new PreprocesadorImagenIa();

    @Test
    void validarFormato_pdfRechazado() {
        MockMultipartFile archivo = new MockMultipartFile(
                "documento", "doc.pdf", "application/pdf", "contenido".getBytes());
        assertThrows(ExcepcionReglaNegocio.class, () -> preprocesador.validarFormato(archivo));
    }

    @Test
    void validarFormato_jpegAceptado() {
        MockMultipartFile archivo = new MockMultipartFile(
                "documento", "doc.jpg", "image/jpeg", "contenido".getBytes());
        preprocesador.validarFormato(archivo); // no debe lanzar
    }

    @Test
    void validarFormato_archivoVacio_esRechazado() {
        MockMultipartFile archivo = new MockMultipartFile("documento", "doc.jpg", "image/jpeg", new byte[0]);
        assertThrows(ExcepcionReglaNegocio.class, () -> preprocesador.validarFormato(archivo));
    }

    @Test
    void validarFormato_contentTypeNulo_esRechazado() {
        MockMultipartFile archivo = new MockMultipartFile("documento", "doc", null, "contenido".getBytes());
        assertThrows(ExcepcionReglaNegocio.class, () -> preprocesador.validarFormato(archivo));
    }

    /**
     * El techo global de multipart subió a 100MB para admitir video de portafolio,
     * así que el límite de verificación vive aquí y debe seguir vigente.
     */
    @Test
    void validarFormato_imagenQueSuperaLos5MB_esRechazada() {
        MockMultipartFile archivo = new MockMultipartFile(
                "documento", "cedula.jpg", "image/jpeg", new byte[6 * 1024 * 1024]);

        ExcepcionReglaNegocio error = assertThrows(ExcepcionReglaNegocio.class,
                () -> preprocesador.validarFormato(archivo));

        assertThat(error).hasMessageContaining("5 MB");
    }

    @Test
    void validarFormato_imagenJustoBajoElLimite_esAceptada() {
        MockMultipartFile archivo = new MockMultipartFile(
                "documento", "cedula.jpg", "image/jpeg", new byte[5 * 1024 * 1024]);

        preprocesador.validarFormato(archivo); // no debe lanzar
    }

    @Test
    void comprimirParaIa_imagenPequenaSigueSiendoValida() throws Exception {
        byte[] original = imagenSolidaComoPng(200, 200);

        byte[] resultado = preprocesador.comprimirParaIa(original);

        assertThat(resultado.length).isLessThanOrEqualTo(180_000);
        assertThat(ImageIO.read(new ByteArrayInputStream(resultado))).isNotNull();
    }

    @Test
    void comprimirParaIa_imagenGrande_seEscalaHastaCumplirElLimite() throws Exception {
        byte[] original = imagenSolidaComoPng(4000, 3000);

        byte[] resultado = preprocesador.comprimirParaIa(original);

        assertThat(resultado.length).isLessThanOrEqualTo(180_000);
    }

    @Test
    void comprimirParaIa_bytesNoSonUnaImagen_lanzaExcepcionReglaNegocio() {
        assertThrows(ExcepcionReglaNegocio.class, () -> preprocesador.comprimirParaIa("no soy una imagen".getBytes()));
    }

    private byte[] imagenSolidaComoPng(int ancho, int alto) throws Exception {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        var g = imagen.createGraphics();
        g.setColor(java.awt.Color.BLUE);
        g.fillRect(0, 0, ancho, alto);
        g.dispose();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }
}
