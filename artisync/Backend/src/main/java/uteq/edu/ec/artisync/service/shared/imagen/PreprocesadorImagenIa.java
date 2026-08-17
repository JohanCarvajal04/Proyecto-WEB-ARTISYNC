package uteq.edu.ec.artisync.service.shared.imagen;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * Prepara documentos de identidad/certificados para NVIDIA NIM, que acepta
 * ~180 KB en base64. Una foto de cédula desde un móvil pesa varios MB y
 * fallaría con 413 sin este paso.
 */
@Slf4j
@Component
public class PreprocesadorImagenIa {

    private static final Set<String> TIPOS_ACEPTADOS = Set.of("image/jpeg", "image/png");
    private static final int LIMITE_BYTES = 180_000;

    /**
     * Tope por documento de verificación. Antes lo imponía el límite global de
     * multipart, pero ese subió para admitir el video del portafolio: una cédula
     * sigue siendo una foto de pocos MB y el límite le corresponde a este caso de uso.
     */
    private static final long LIMITE_SUBIDA_BYTES = 5L * 1024 * 1024;
    private static final float CALIDAD_INICIAL = 0.85f;
    private static final float CALIDAD_MINIMA = 0.25f;
    private static final int MAX_INTENTOS = 6;

    /** Validación barata al subir: solo revisa el tipo declarado por el cliente. */
    public void validarFormato(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ExcepcionReglaNegocio("El documento está vacío.");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !TIPOS_ACEPTADOS.contains(tipo)) {
            throw new ExcepcionReglaNegocio(
                    "Formato de documento no soportado: " + tipo
                            + ". Se aceptan image/jpeg o image/png.");
        }
        if (archivo.getSize() > LIMITE_SUBIDA_BYTES) {
            throw new ExcepcionReglaNegocio(
                    "El documento supera el máximo de 5 MB permitido para verificación.");
        }
    }

    /** Recomprime detectando el formato por los bytes, no por un header de cliente. */
    public byte[] comprimirParaIa(byte[] original) {
        BufferedImage imagen;
        try {
            imagen = ImageIO.read(new ByteArrayInputStream(original));
        } catch (IOException e) {
            throw new ExcepcionReglaNegocio("El documento almacenado no es una imagen legible.");
        }
        if (imagen == null) {
            throw new ExcepcionReglaNegocio("El documento almacenado no es una imagen legible.");
        }

        float calidad = CALIDAD_INICIAL;
        byte[] resultado = comprimirJpeg(imagen, calidad);

        int intentos = 0;
        while (resultado.length > LIMITE_BYTES && intentos < MAX_INTENTOS) {
            if (calidad > CALIDAD_MINIMA) {
                calidad -= 0.15f;
            } else {
                imagen = escalar(imagen, 0.75);
                calidad = CALIDAD_INICIAL;
            }
            resultado = comprimirJpeg(imagen, calidad);
            intentos++;
        }

        if (resultado.length > LIMITE_BYTES) {
            throw new ExcepcionReglaNegocio(
                    "No fue posible comprimir el documento por debajo de " + LIMITE_BYTES + " bytes.");
        }
        return resultado;
    }

    private byte[] comprimirJpeg(BufferedImage imagen, float calidad) {
        BufferedImage sinAlfa = quitarCanalAlfa(imagen);
        Iterator<ImageWriter> escritores = ImageIO.getImageWritersByFormatName("jpg");
        if (!escritores.hasNext()) {
            throw new IllegalStateException("No hay un ImageWriter de JPEG disponible en esta JVM.");
        }
        ImageWriter escritor = escritores.next();
        ImageWriteParam parametros = escritor.getDefaultWriteParam();
        parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        parametros.setCompressionQuality(calidad);

        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
             ImageOutputStream flujo = ImageIO.createImageOutputStream(salida)) {
            escritor.setOutput(flujo);
            escritor.write(null, new IIOImage(sinAlfa, null, null), parametros);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al comprimir la imagen del documento.", e);
        } finally {
            escritor.dispose();
        }
    }

    private BufferedImage quitarCanalAlfa(BufferedImage original) {
        if (!original.getColorModel().hasAlpha()) {
            return original;
        }
        BufferedImage sinAlfa = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sinAlfa.createGraphics();
        g.drawImage(original, 0, 0, Color.WHITE, null);
        g.dispose();
        return sinAlfa;
    }

    private BufferedImage escalar(BufferedImage original, double factor) {
        int nuevoAncho = Math.max(1, (int) (original.getWidth() * factor));
        int nuevoAlto = Math.max(1, (int) (original.getHeight() * factor));
        BufferedImage escalada = new BufferedImage(nuevoAncho, nuevoAlto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = escalada.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(original, 0, 0, nuevoAncho, nuevoAlto, null);
        g.dispose();
        return escalada;
    }
}
