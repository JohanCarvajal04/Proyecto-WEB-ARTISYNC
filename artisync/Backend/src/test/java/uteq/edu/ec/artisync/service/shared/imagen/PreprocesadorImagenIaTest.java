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

    /**
     * Las pruebas de "imagen grande" de arriba usan un color sólido: entropía
     * casi nula, comprime trivialmente bien y no valida el caso real (foto de
     * cédula con texto/rostro). Esta reproduce una foto de móvil típica —
     * ruido aleatorio de alta entropía, JPEG lo comprime mucho peor por
     * calidad — para confirmar si el bucle de compresión converge dentro de
     * los 180 KB en un escenario realista, no solo en el mejor caso.
     */
    @Test
    void comprimirParaIa_imagenGrandeDeAltaEntropia_convergeODaMensajeClaro() throws Exception {
        byte[] original = imagenRuidoComoPng(3000, 2000);

        try {
            byte[] resultado = preprocesador.comprimirParaIa(original);
            assertThat(resultado.length).isLessThanOrEqualTo(180_000);
            assertThat(ImageIO.read(new ByteArrayInputStream(resultado))).isNotNull();
        } catch (ExcepcionReglaNegocio e) {
            // Documenta el límite real: si esto se dispara, es evidencia de que
            // MAX_INTENTOS/LIMITE_BYTES sí puede fallar con fotos reales de
            // alta entropía, no solo en teoría.
            assertThat(e).hasMessageContaining("No fue posible comprimir");
        }
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

    private byte[] imagenRuidoComoPng(int ancho, int alto) throws Exception {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        java.util.Random aleatorio = new java.util.Random(42);
        for (int x = 0; x < ancho; x++) {
            for (int y = 0; y < alto; y++) {
                imagen.setRGB(x, y, aleatorio.nextInt(0xFFFFFF));
            }
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        ImageIO.write(imagen, "png", salida);
        return salida.toByteArray();
    }
}
