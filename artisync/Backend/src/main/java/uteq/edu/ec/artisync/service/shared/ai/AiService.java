package uteq.edu.ec.artisync.service.shared.ai;

import uteq.edu.ec.artisync.dto.ai.*;

import java.util.List;

/**
 * Contract Strategy de IA. Los seis métodos existen aunque hoy solo se
 * cableen verifyIdentity/analyzeCertificate (REQ-F-006/007); el resto
 * queda listo para futuras herramientas del moderador sin romper la interfaz.
 */
public interface AiService {

    AiVerificationResponse verifyIdentity(byte[] imagenBytes, String mimeType);

    AiVerificationResponse analyzeCertificate(byte[] imagenBytes, String mimeType);

    IaModeracionResponse moderarContenido(String textoMensaje);

    AiClassificationResponse classifyOffering(String titulo, String descripcion,
                                                List<String> categoriasDisponibles);

    List<String> sugerirPreguntasBriefing(String categoria, String titulo, String descripcion);

    AiReviewResponse analyzeReview(String textoResena, int estrellas);
}
