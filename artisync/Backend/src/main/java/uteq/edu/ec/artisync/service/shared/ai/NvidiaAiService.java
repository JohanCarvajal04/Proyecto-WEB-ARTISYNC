package uteq.edu.ec.artisync.service.shared.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import uteq.edu.ec.artisync.config.AiProperties;
import uteq.edu.ec.artisync.dto.ai.*;
import uteq.edu.ec.artisync.exception.AiServiceUnavailableException;

import java.math.BigDecimal;
import java.util.*;

@Service
@ConditionalOnProperty(name = "ia.provider", havingValue = "nvidia")
@Slf4j
public class NvidiaAiService extends AbstractAiService implements AiService {

    private final RestClient restClient;
    private final AiProperties.NvidiaConfig config;
    private final ObjectMapper objectMapper;

    /**
     * Construye el cliente de NVIDIA NIM validando de entrada que haya una API key
     * configurada — falla rápido al arrancar en vez de fallar en la primera
     * petición real de un usuario.
     *
     * @param restClient cliente HTTP compartido para llamadas a proveedores de IA
     * @param iaProperties configuración de proveedores de IA, de la que se toma la sección de NVIDIA
     * @param objectMapper mapeador JSON usado para construir y leer las peticiones/respuestas de NVIDIA
     * @throws IllegalStateException si no hay una API key de NVIDIA configurada
     */
    public NvidiaAiService(@Qualifier("iaRestClient") RestClient restClient,
                            AiProperties iaProperties,
                            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.config = iaProperties.getNvidia();
        this.objectMapper = objectMapper;
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "ia.provider=nvidia requiere ia.nvidia.api-key (variable NVIDIA_API_KEY); "
                            + "el backend no puede arrancar sin ella.");
        }
        log.info("Offering de IA NVIDIA NIM inicializado [modelo={}]", config.getModel());
    }

    /**
     * {@inheritDoc}
     * @param imagenBytes el imagen bytes
     * @param mimeType el mime type
     * @return el resultado de la operacion, de tipo {@code AiVerificationResponse}
     */
    @Override
    public AiVerificationResponse verifyIdentity(byte[] imagenBytes, String mimeType) {
        String prompt = loadPrompt("prompt_verificacion_identidad.md");
        String respuesta = callNvidiaWithImage(prompt, imagenBytes, mimeType);
        return parseStrictVerification(respuesta, true);
    }

    /**
     * {@inheritDoc}
     * @param imagenBytes el imagen bytes
     * @param mimeType el mime type
     * @return el resultado de la operacion, de tipo {@code AiVerificationResponse}
     */
    @Override
    public AiVerificationResponse analyzeCertificate(byte[] imagenBytes, String mimeType) {
        String prompt = loadPrompt("prompt_verificacion_certificado.md");
        String respuesta = callNvidiaWithImage(prompt, imagenBytes, mimeType);
        return parseStrictVerification(respuesta, false);
    }

    /**
     * {@inheritDoc}
     * Implementación NVIDIA NIM: ante cualquier error de IA se degrada a
     * "apropiado" en vez de bloquear el mensaje (fail-open).
     * @param textoMensaje el texto mensaje
     * @return el resultado de la operacion, de tipo {@code IaModeracionResponse}
     */
    @Override
    public IaModeracionResponse moderarContenido(String textoMensaje) {
        String prompt = loadPrompt("prompt_moderacion_mensaje.md", sanitizeForPrompt(textoMensaje));
        try {
            JsonNode nodo = objectMapper.readTree(extractJson(conReintentoTransitorio(() -> llamarNvidiaSoloTexto(prompt))));
            return IaModeracionResponse.builder()
                    .esApropiado(nodo.path("es_apropiado").asBoolean(true))
                    .categoriaInfraccion(nodo.path("categoria_infraccion").asString("ninguno"))
                    .confianza(acotarConfianza(toBigDecimal(nodo.path("confianza").asDouble(0.5))))
                    .razon(textoONull(nodo, "razon"))
                    .build();
        } catch (Exception e) {
            log.error("[NVIDIA] Error al moderar contenido: {}", e.getMessage());
            return IaModeracionResponse.builder()
                    .esApropiado(true).categoriaInfraccion("ninguno")
                    .confianza(BigDecimal.ZERO).razon("Error al procesar con IA").build();
        }
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
        String categorias = String.join(", ", categoriasDisponibles);
        String prompt = loadPrompt("prompt_clasificacion_servicio.md", categorias,
                sanitizeForPrompt(titulo), sanitizeForPrompt(descripcion));
        try {
            JsonNode nodo = objectMapper.readTree(extractJson(conReintentoTransitorio(() -> llamarNvidiaSoloTexto(prompt))));
            List<String> etiquetas = new ArrayList<>();
            nodo.path("etiquetas_sugeridas").forEach(e -> etiquetas.add(e.asString()));
            return AiClassificationResponse.builder()
                    .categoriaSugerida(nodo.path("categoria_sugerida").asString(""))
                    .subcategoriaSugerida(nodo.path("subcategoria_sugerida").asString(""))
                    .etiquetasSugeridas(etiquetas)
                    .confianza(acotarConfianza(toBigDecimal(nodo.path("confianza").asDouble(0.5))))
                    .build();
        } catch (Exception e) {
            log.error("[NVIDIA] Error al clasificar servicio: {}", e.getMessage());
            return AiClassificationResponse.builder()
                    .categoriaSugerida("Sin categoría").subcategoriaSugerida("General")
                    .etiquetasSugeridas(List.of()).confianza(BigDecimal.ZERO).build();
        }
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
        String prompt = loadPrompt("prompt_sugerencia_briefing.md", sanitizeForPrompt(categoria),
                sanitizeForPrompt(titulo), sanitizeForPrompt(descripcion));
        try {
            JsonNode nodo = objectMapper.readTree(extractJson(conReintentoTransitorio(() -> llamarNvidiaSoloTexto(prompt))));
            List<String> preguntas = new ArrayList<>();
            nodo.path("preguntas").forEach(p -> preguntas.add(p.asString()));
            return preguntas.isEmpty() ? List.of("¿Qué necesitas?") : preguntas;
        } catch (Exception e) {
            log.error("[NVIDIA] Error al generar briefing: {}", e.getMessage());
            return List.of("¿Cuál es el objetivo del proyecto?",
                    "¿Cuál es la fecha de entrega deseada?", "¿Tienes referencias visuales?");
        }
    }

    /**
     * {@inheritDoc}
     * @param textoResena el texto resena
     * @param estrellas los estrellas
     * @return el resultado de la operacion, de tipo {@code AiReviewResponse}
     */
    @Override
    public AiReviewResponse analyzeReview(String textoResena, int estrellas) {
        String prompt = loadPrompt("prompt_analisis_resena.md", estrellas, sanitizeForPrompt(textoResena));
        try {
            JsonNode nodo = objectMapper.readTree(extractJson(conReintentoTransitorio(() -> llamarNvidiaSoloTexto(prompt))));
            return AiReviewResponse.builder()
                    .sentimiento(nodo.path("sentimiento").asString("neutro"))
                    .esCoherenteConEstrellas(nodo.path("es_coherente_con_estrellas").asBoolean(true))
                    .esSpam(nodo.path("es_spam").asBoolean(false))
                    .esInapropiado(nodo.path("es_inapropiado").asBoolean(false))
                    .confianza(acotarConfianza(toBigDecimal(nodo.path("confianza").asDouble(0.5))))
                    .razon(textoONull(nodo, "razon"))
                    .build();
        } catch (Exception e) {
            log.error("[NVIDIA] Error al analizar reseña: {}", e.getMessage());
            return AiReviewResponse.builder()
                    .sentimiento("neutro").esCoherenteConEstrellas(true)
                    .esSpam(false).esInapropiado(false).confianza(BigDecimal.ZERO).build();
        }
    }

    private String llamarNvidiaSoloTexto(String prompt) {
        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 1.0, "top_p", 0.95, "max_tokens", 8192, "stream", false);
        return executeCall(body);
    }

    private String callNvidiaWithImage(String prompt, byte[] imagenBytes, String mimeType) {
        String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imagenBytes);
        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "messages", List.of(Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text", prompt),
                        Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))))),
                "temperature", 1.0, "top_p", 0.95, "max_tokens", 8192, "stream", false);
        return executeCall(body);
    }

    private String executeCall(Map<String, Object> requestBody) {
        String url = config.getBaseUrl() + "/chat/completions";
        try {
            log.info("[NVIDIA] Enviando solicitud a {} [payload={} bytes]", url, estimarTamanoPayload(requestBody));
            String responseBody = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
            JsonNode raiz = objectMapper.readTree(responseBody);
            return raiz.path("choices").path(0).path("message").path("content").asString("");
        } catch (HttpClientErrorException.Unauthorized e) {
            log.warn("[NVIDIA] 401 Unauthorized. Body de respuesta: {}", e.getResponseBodyAsString());
            throw new AiServiceUnavailableException("NVIDIA rechazó la API key configurada (401).", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            log.warn("[NVIDIA] 429 Too Many Requests. Body de respuesta: {}", e.getResponseBodyAsString());
            throw new AiServiceUnavailableException("Se alcanzó el límite de solicitudes de NVIDIA (429).", e, true);
        } catch (HttpClientErrorException e) {
            log.warn("[NVIDIA] {} de cliente. Body de respuesta: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            if (e.getStatusCode().value() == 413) {
                throw new AiServiceUnavailableException("El documento supera el límite de NVIDIA (413).", e);
            }
            throw new AiServiceUnavailableException(
                    "NVIDIA rechazó la solicitud (" + e.getStatusCode().value() + ").", e);
        } catch (HttpServerErrorException e) {
            log.warn("[NVIDIA] Error de servidor {}. Body de respuesta: {}", e.getStatusCode().value(), e.getResponseBodyAsString());
            throw new AiServiceUnavailableException("NVIDIA respondió con un error de servidor.", e);
        } catch (ResourceAccessException e) {
            log.warn("[NVIDIA] Tiempo de espera agotado: {}", e.getMessage());
            throw new AiServiceUnavailableException("Tiempo de espera agotado al contactar a NVIDIA.", e, true);
        } catch (AiServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceUnavailableException("Error inesperado al comunicarse con NVIDIA NIM.", e);
        }
    }

    /** Tamaño aproximado del cuerpo JSON, solo para diagnóstico — no exacto al byte. */
    private int estimarTamanoPayload(Map<String, Object> requestBody) {
        try {
            return objectMapper.writeValueAsBytes(requestBody).length;
        } catch (Exception e) {
            return -1;
        }
    }

    private AiVerificationResponse parseStrictVerification(String respuestaJson, boolean esIdentidad) {
        try {
            JsonNode nodo = objectMapper.readTree(extractJson(respuestaJson));
            String campoValido = esIdentidad ? "es_documento_valido" : "es_certificado_valido";
            return AiVerificationResponse.builder()
                    .aprobado(nodo.path(campoValido).asBoolean(false))
                    .confianza(acotarConfianza(toBigDecimal(nodo.path("confianza").asDouble(0.0))))
                    .tipoDocumento(textoONull(nodo, esIdentidad ? "tipo_documento" : "tipo_certificado"))
                    .nombreDetectado(textoONull(nodo, esIdentidad ? "nombre_detectado" : "nombre_titular"))
                    .mayorEdad(esIdentidad ? nodo.path("es_mayor_de_edad").asBoolean(false) : null)
                    .fechaNacimiento(esIdentidad ? textoONull(nodo, "fecha_nacimiento") : null)
                    .paisEmision(esIdentidad ? textoONull(nodo, "pais_emision") : null)
                    .institucionEmisora(esIdentidad ? null : textoONull(nodo, "institucion_emisora"))
                    .campoEstudio(esIdentidad ? null : textoONull(nodo, "campo_estudio"))
                    .fechaEmision(esIdentidad ? null : textoONull(nodo, "fecha_emision"))
                    .razonRechazo(textoONull(nodo, "razon_rechazo"))
                    .build();
        } catch (AiServiceUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiServiceUnavailableException("No se pudo interpretar la respuesta de NVIDIA.", e);
        }
    }
}
