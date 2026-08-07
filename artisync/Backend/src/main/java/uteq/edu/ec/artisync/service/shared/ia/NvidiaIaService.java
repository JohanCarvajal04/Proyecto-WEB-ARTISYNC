package uteq.edu.ec.artisync.service.shared.ia;

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
import uteq.edu.ec.artisync.config.IaProperties;
import uteq.edu.ec.artisync.dto.ia.*;
import uteq.edu.ec.artisync.exception.ExcepcionServicioIaNoDisponible;

import java.math.BigDecimal;
import java.util.*;

@Service
@ConditionalOnProperty(name = "ia.provider", havingValue = "nvidia")
@Slf4j
public class NvidiaIaService extends AbstractIaService implements IaService {

    private final RestClient restClient;
    private final IaProperties.NvidiaConfig config;
    private final ObjectMapper objectMapper;

    public NvidiaIaService(@Qualifier("iaRestClient") RestClient restClient,
                            IaProperties iaProperties,
                            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.config = iaProperties.getNvidia();
        this.objectMapper = objectMapper;
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalStateException(
                    "ia.provider=nvidia requiere ia.nvidia.api-key (variable NVIDIA_API_KEY); "
                            + "el backend no puede arrancar sin ella.");
        }
        log.info("Servicio de IA NVIDIA NIM inicializado [modelo={}]", config.getModel());
    }

    @Override
    public IaVerificacionResponse verificarIdentidad(byte[] imagenBytes, String mimeType) {
        String prompt = cargarPrompt("prompt_verificacion_identidad.md");
        String respuesta = llamarNvidiaConImagen(prompt, imagenBytes, mimeType);
        return parsearVerificacionEstricto(respuesta, true);
    }

    @Override
    public IaVerificacionResponse analizarCertificado(byte[] imagenBytes, String mimeType) {
        String prompt = cargarPrompt("prompt_verificacion_certificado.md");
        String respuesta = llamarNvidiaConImagen(prompt, imagenBytes, mimeType);
        return parsearVerificacionEstricto(respuesta, false);
    }

    @Override
    public IaModeracionResponse moderarContenido(String textoMensaje) {
        String prompt = cargarPrompt("prompt_moderacion_mensaje.md", textoMensaje);
        try {
            JsonNode nodo = objectMapper.readTree(extraerJson(llamarNvidiaSoloTexto(prompt)));
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

    @Override
    public IaClasificacionResponse clasificarServicio(String titulo, String descripcion, List<String> categoriasDisponibles) {
        String categorias = String.join(", ", categoriasDisponibles);
        String prompt = cargarPrompt("prompt_clasificacion_servicio.md", categorias, titulo, descripcion);
        try {
            JsonNode nodo = objectMapper.readTree(extraerJson(llamarNvidiaSoloTexto(prompt)));
            List<String> etiquetas = new ArrayList<>();
            nodo.path("etiquetas_sugeridas").forEach(e -> etiquetas.add(e.asString()));
            return IaClasificacionResponse.builder()
                    .categoriaSugerida(nodo.path("categoria_sugerida").asString(""))
                    .subcategoriaSugerida(nodo.path("subcategoria_sugerida").asString(""))
                    .etiquetasSugeridas(etiquetas)
                    .confianza(acotarConfianza(toBigDecimal(nodo.path("confianza").asDouble(0.5))))
                    .build();
        } catch (Exception e) {
            log.error("[NVIDIA] Error al clasificar servicio: {}", e.getMessage());
            return IaClasificacionResponse.builder()
                    .categoriaSugerida("Sin categoría").subcategoriaSugerida("General")
                    .etiquetasSugeridas(List.of()).confianza(BigDecimal.ZERO).build();
        }
    }

    @Override
    public List<String> sugerirPreguntasBriefing(String categoria, String titulo, String descripcion) {
        String prompt = cargarPrompt("prompt_sugerencia_briefing.md", categoria, titulo, descripcion);
        try {
            JsonNode nodo = objectMapper.readTree(extraerJson(llamarNvidiaSoloTexto(prompt)));
            List<String> preguntas = new ArrayList<>();
            nodo.path("preguntas").forEach(p -> preguntas.add(p.asString()));
            return preguntas.isEmpty() ? List.of("¿Qué necesitas?") : preguntas;
        } catch (Exception e) {
            log.error("[NVIDIA] Error al generar briefing: {}", e.getMessage());
            return List.of("¿Cuál es el objetivo del proyecto?",
                    "¿Cuál es la fecha de entrega deseada?", "¿Tienes referencias visuales?");
        }
    }

    @Override
    public IaResenaResponse analizarResena(String textoResena, int estrellas) {
        String prompt = cargarPrompt("prompt_analisis_resena.md", estrellas, textoResena);
        try {
            JsonNode nodo = objectMapper.readTree(extraerJson(llamarNvidiaSoloTexto(prompt)));
            return IaResenaResponse.builder()
                    .sentimiento(nodo.path("sentimiento").asString("neutro"))
                    .esCoherenteConEstrellas(nodo.path("es_coherente_con_estrellas").asBoolean(true))
                    .esSpam(nodo.path("es_spam").asBoolean(false))
                    .esInapropiado(nodo.path("es_inapropiado").asBoolean(false))
                    .confianza(acotarConfianza(toBigDecimal(nodo.path("confianza").asDouble(0.5))))
                    .razon(textoONull(nodo, "razon"))
                    .build();
        } catch (Exception e) {
            log.error("[NVIDIA] Error al analizar reseña: {}", e.getMessage());
            return IaResenaResponse.builder()
                    .sentimiento("neutro").esCoherenteConEstrellas(true)
                    .esSpam(false).esInapropiado(false).confianza(BigDecimal.ZERO).build();
        }
    }

    private String llamarNvidiaSoloTexto(String prompt) {
        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "temperature", 1.0, "top_p", 0.95, "max_tokens", 8192, "stream", false);
        return ejecutarLlamada(body);
    }

    private String llamarNvidiaConImagen(String prompt, byte[] imagenBytes, String mimeType) {
        String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imagenBytes);
        Map<String, Object> body = Map.of(
                "model", config.getModel(),
                "messages", List.of(Map.of("role", "user", "content", List.of(
                        Map.of("type", "text", "text", prompt),
                        Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))))),
                "temperature", 1.0, "top_p", 0.95, "max_tokens", 8192, "stream", false);
        return ejecutarLlamada(body);
    }

    private String ejecutarLlamada(Map<String, Object> requestBody) {
        String url = config.getBaseUrl() + "/chat/completions";
        try {
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
            throw new ExcepcionServicioIaNoDisponible("NVIDIA rechazó la API key configurada (401).", e);
        } catch (HttpClientErrorException.TooManyRequests e) {
            throw new ExcepcionServicioIaNoDisponible("Se alcanzó el límite de solicitudes de NVIDIA (429).", e);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 413) {
                throw new ExcepcionServicioIaNoDisponible("El documento supera el límite de NVIDIA (413).", e);
            }
            throw new ExcepcionServicioIaNoDisponible(
                    "NVIDIA rechazó la solicitud (" + e.getStatusCode().value() + ").", e);
        } catch (HttpServerErrorException e) {
            throw new ExcepcionServicioIaNoDisponible("NVIDIA respondió con un error de servidor.", e);
        } catch (ResourceAccessException e) {
            throw new ExcepcionServicioIaNoDisponible("Tiempo de espera agotado al contactar a NVIDIA.", e);
        } catch (ExcepcionServicioIaNoDisponible e) {
            throw e;
        } catch (Exception e) {
            throw new ExcepcionServicioIaNoDisponible("Error inesperado al comunicarse con NVIDIA NIM.", e);
        }
    }

    private IaVerificacionResponse parsearVerificacionEstricto(String respuestaJson, boolean esIdentidad) {
        try {
            JsonNode nodo = objectMapper.readTree(extraerJson(respuestaJson));
            String campoValido = esIdentidad ? "es_documento_valido" : "es_certificado_valido";
            return IaVerificacionResponse.builder()
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
        } catch (ExcepcionServicioIaNoDisponible e) {
            throw e;
        } catch (Exception e) {
            throw new ExcepcionServicioIaNoDisponible("No se pudo interpretar la respuesta de NVIDIA.", e);
        }
    }
}
