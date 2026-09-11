package uteq.edu.ec.artisync.service.shared.almacenamiento;

import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.exception.BusinessRuleException;

import java.util.Set;

/**
 * Qué puede subirse en cada caso de uso. El techo de multipart es global y está
 * dimensionado para el archivo más grande que admite la plataforma (video de
 * portafolio); estos límites son los que de verdad aplican, porque un entregable
 * de 100MB es legítimo y una foto de perfil del mismo tamaño no.
 *
 * <p>La validación es sobre el content-type declarado por el cliente, así que no
 * es una garantía sobre el contenido real: sirve para rechazar lo evidente antes
 * de gastar ancho de banda, no como control de seguridad.
 */
public record FilePolicy(Set<String> tiposPermitidos, long maxBytes, String descripcion) {

    private static final long MB = 1024L * 1024L;

    private static final Set<String> IMAGENES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif");

    private static final Set<String> VIDEOS = Set.of(
            "video/mp4", "video/webm", "video/quicktime");

    private static final Set<String> DOCUMENTOS = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    /** Obras de portafolio: imagen o video, que es lo que se exhibe. */
    public static final FilePolicy PORTAFOLIO = new FilePolicy(
            union(IMAGENES, VIDEOS), 100 * MB, "imagen o video");

    /** Entregables: el trabajo final, que puede ser cualquiera de los tres. */
    public static final FilePolicy ENTREGABLE = new FilePolicy(
            union(union(IMAGENES, VIDEOS), DOCUMENTOS), 100 * MB, "imagen, video o documento");

    /**
     * Foto de perfil: solo imagen, y a propósito sin SVG (IMAGENES no lo incluye):
     * se sirve inline desde un endpoint público, así que un SVG con script
     * embebido sería XSS almacenado.
     */
    public static final FilePolicy PERFIL = new FilePolicy(IMAGENES, 5 * MB, "imagen");

    private static Set<String> union(Set<String> a, Set<String> b) {
        return java.util.stream.Stream.concat(a.stream(), b.stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * Comprueba el cumplimiento de restricciones o formatos sobre los datos provistos.
     *
     * @param archivo objeto binario multipart representando el documento o medio fisico
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessRuleException("El archivo está vacío.");
        }
        String tipo = archivo.getContentType();
        String normalizado = tipo == null ? "" : tipo.split(";")[0].trim().toLowerCase();
        if (!tiposPermitidos.contains(normalizado)) {
            throw new BusinessRuleException(
                    "Formato no soportado: " + tipo + ". Se acepta " + descripcion + ".");
        }
        if (archivo.getSize() > maxBytes) {
            throw new BusinessRuleException(
                    "El archivo supera el máximo de " + (maxBytes / MB) + " MB.");
        }
    }
}
