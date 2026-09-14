package uteq.edu.ec.artisync.service.shared.ai;

import uteq.edu.ec.artisync.dto.ai.*;

import java.util.List;

/**
 * Contract Strategy de IA. Los seis métodos existen aunque hoy solo se
 * cableen verifyIdentity/analyzeCertificate (REQ-F-006/007); el resto
 * queda listo para futuras herramientas del moderador sin romper la interfaz.
 */
public interface AiService {

    /**
     * Analiza un documento de identidad y determina si acredita la mayoría de edad (RNF-12/REQ-F-006).
     * @param imagenBytes contenido binario de la imagen del documento
     * @param mimeType tipo MIME de la imagen (por ejemplo {@code image/jpeg})
     * @return veredicto de la IA con puntaje de confianza y motivo
     */
    AiVerificationResponse verifyIdentity(byte[] imagenBytes, String mimeType);

    /**
     * Analiza un certificado profesional cargado por el creador (REQ-F-007).
     * @param imagenBytes contenido binario de la imagen del certificado
     * @param mimeType tipo MIME de la imagen (por ejemplo {@code image/png})
     * @return veredicto de la IA con puntaje de confianza y motivo
     */
    AiVerificationResponse analyzeCertificate(byte[] imagenBytes, String mimeType);

    /**
     * Modera el texto de un mensaje de chat, detectando contenido inapropiado.
     * @param textoMensaje contenido textual del mensaje a evaluar
     * @return veredicto de moderación de la IA
     */
    IaModeracionResponse moderarContenido(String textoMensaje);

    /**
     * Sugiere la categoría más adecuada para un servicio a partir de su título y descripción.
     * @param titulo título del servicio publicado
     * @param descripcion descripción del servicio publicado
     * @param categoriasDisponibles catálogo de categorías entre las que la IA debe elegir
     * @return clasificación sugerida por la IA
     */
    AiClassificationResponse classifyOffering(String titulo, String descripcion,
                                                List<String> categoriasDisponibles);

    /**
     * Sugiere preguntas de briefing pertinentes para un servicio, según su categoría.
     * @param categoria categoría del servicio
     * @param titulo título del servicio
     * @param descripcion descripción del servicio
     * @return lista de preguntas sugeridas para el formulario de briefing
     */
    List<String> sugerirPreguntasBriefing(String categoria, String titulo, String descripcion);

    /**
     * Analiza el texto de una reseña junto con su calificación en estrellas.
     * @param textoResena contenido textual de la reseña
     * @param estrellas calificación otorgada (1 a 5)
     * @return análisis de la IA sobre la reseña
     */
    AiReviewResponse analyzeReview(String textoResena, int estrellas);
}
