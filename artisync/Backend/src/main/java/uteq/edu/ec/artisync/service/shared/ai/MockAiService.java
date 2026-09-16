package uteq.edu.ec.artisync.service.shared.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uteq.edu.ec.artisync.dto.ai.*;

import java.math.BigDecimal;
import java.util.List;

/** Simula respuestas de IA sin llamadas de red. Activo por defecto (ia.provider=mock). */
@Service
@ConditionalOnProperty(name = "ia.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockAiService extends AbstractAiService implements AiService {

    /**
     * Construye el servicio mock e informa por log que no se realizarán llamadas
     * reales a proveedores de IA — es el proveedor activo por defecto ({@code ia.provider=mock}).
     */
    public MockAiService() {
        log.info("Offering de IA MOCK inicializado — no se realizarán llamadas reales a APIs de IA");
    }

    /**
     * {@inheritDoc}
     * @param imagenBytes el imagen bytes
     * @param mimeType el mime type
     * @return el resultado de la operacion, de tipo {@code AiVerificationResponse}
     */
    @Override
    public AiVerificationResponse verifyIdentity(byte[] imagenBytes, String mimeType) {
        return AiVerificationResponse.builder()
                .aprobado(true)
                .confianza(new BigDecimal("0.92"))
                .tipoDocumento("cedula")
                .nombreDetectado("User de Prueba")
                .mayorEdad(true)
                .fechaNacimiento("1995-05-20")
                .paisEmision("Ecuador")
                .build();
    }

    /**
     * {@inheritDoc}
     * @param imagenBytes el imagen bytes
     * @param mimeType el mime type
     * @return el resultado de la operacion, de tipo {@code AiVerificationResponse}
     */
    @Override
    public AiVerificationResponse analyzeCertificate(byte[] imagenBytes, String mimeType) {
        return AiVerificationResponse.builder()
                .aprobado(true)
                .confianza(new BigDecimal("0.88"))
                .tipoDocumento("titulo_universitario")
                .nombreDetectado("User de Prueba")
                .institucionEmisora("Universidad de Prueba")
                .campoEstudio("Diseño Gráfico")
                .fechaEmision("2020-07-15")
                .build();
    }

    /**
     * {@inheritDoc}
     * @param textoMensaje el texto mensaje
     * @return el resultado de la operacion, de tipo {@code IaModeracionResponse}
     */
    @Override
    public IaModeracionResponse moderarContenido(String textoMensaje) {
        return IaModeracionResponse.builder()
                .esApropiado(true).categoriaInfraccion("ninguno")
                .confianza(new BigDecimal("0.95")).build();
    }

    /**
     * {@inheritDoc}
     * @param titulo el titulo
     * @param descripcion el descripcion
     * @param categoriasDisponibles el categorias disponibles
     * @return el resultado de la operacion, de tipo {@code AiClassificationResponse}
     */
    @Override
    public AiClassificationResponse classifyOffering(String titulo, String descripcion, List<String> categoriasDisponibles) {
        String categoria = categoriasDisponibles.isEmpty() ? "General" : categoriasDisponibles.get(0);
        return AiClassificationResponse.builder()
                .categoriaSugerida(categoria).subcategoriaSugerida("General")
                .etiquetasSugeridas(List.of("diseño", "creativo"))
                .confianza(new BigDecimal("0.80")).build();
    }

    /**
     * {@inheritDoc}
     * @param categoria el categoria
     * @param titulo el titulo
     * @param descripcion el descripcion
     * @return la lista de String encontrados
     */
    @Override
    public List<String> sugerirPreguntasBriefing(String categoria, String titulo, String descripcion) {
        return List.of("¿Cuál es el objetivo del proyecto?", "¿Tienes referencias visuales?");
    }

    /**
     * {@inheritDoc}
     * @param textoResena el texto resena
     * @param estrellas los estrellas
     * @return el resultado de la operacion, de tipo {@code AiReviewResponse}
     */
    @Override
    public AiReviewResponse analyzeReview(String textoResena, int estrellas) {
        return AiReviewResponse.builder()
                .sentimiento(estrellas >= 4 ? "positivo" : "neutro")
                .esCoherenteConEstrellas(true).esSpam(false).esInapropiado(false)
                .confianza(new BigDecimal("0.90")).build();
    }
}
