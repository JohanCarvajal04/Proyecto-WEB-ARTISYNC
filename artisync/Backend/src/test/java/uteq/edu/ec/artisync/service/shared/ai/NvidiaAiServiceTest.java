package uteq.edu.ec.artisync.service.shared.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uteq.edu.ec.artisync.config.AiProperties;
import uteq.edu.ec.artisync.dto.ai.AiClassificationResponse;
import uteq.edu.ec.artisync.dto.ai.AiReviewResponse;
import uteq.edu.ec.artisync.dto.ai.AiVerificationResponse;
import uteq.edu.ec.artisync.dto.ai.IaModeracionResponse;
import uteq.edu.ec.artisync.exception.AiServiceUnavailableException;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class NvidiaAiServiceTest {

    private MockRestServiceServer servidorSimulado;
    private NvidiaAiService servicio;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidorSimulado = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        AiProperties propiedades = new AiProperties();
        propiedades.getNvidia().setApiKey("nvapi-test-key");
        propiedades.getNvidia().setBaseUrl("https://integrate.api.nvidia.com/v1");

        servicio = new NvidiaAiService(restClient, propiedades, new tools.jackson.databind.ObjectMapper());
    }

    @Test
    void verificarIdentidad_respuestaValida_parseaDictamenCompleto() throws Exception {
        String contenidoIa = new ObjectMapper().writeValueAsString(Map.of(
                "es_documento_valido", true,
                "tipo_documento", "cedula",
                "nombre_detectado", "Ana Pérez",
                "confianza", 0.91));
        String respuestaNvidia = new ObjectMapper().writeValueAsString(
                Map.of("choices", java.util.List.of(
                        Map.of("message", Map.of("content", contenidoIa)))));

        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer nvapi-test-key"))
                .andExpect(jsonPath("$.messages[0].content[1].image_url.url").value(org.hamcrest.Matchers.startsWith("data:image/jpeg;base64,")))
                .andRespond(withSuccess(respuestaNvidia, MediaType.APPLICATION_JSON));

        AiVerificationResponse resultado = servicio.verifyIdentity("contenido-imagen".getBytes(), "image/jpeg");

        assertThat(resultado.isAprobado()).isTrue();
        assertThat(resultado.getNombreDetectado()).isEqualTo("Ana Pérez");
        assertThat(resultado.getConfianza()).isEqualByComparingTo("0.91");
    }

    @Test
    void verificarIdentidad_nvidiaResponde500_lanzaExcepcionServicioNoDisponible() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withServerError());

        assertThrows(AiServiceUnavailableException.class,
                () -> servicio.verifyIdentity("bytes".getBytes(), "image/jpeg"));
    }

    @Test
    void verificarIdentidad_nvidiaResponde401_lanzaExcepcionServicioNoDisponible() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AiServiceUnavailableException.class,
                () -> servicio.verifyIdentity("bytes".getBytes(), "image/jpeg"));
    }

    @Test
    void verificarIdentidad_nvidiaResponde401_noEsReintentable() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        AiServiceUnavailableException error = org.junit.jupiter.api.Assertions.assertThrows(
                AiServiceUnavailableException.class,
                () -> servicio.verifyIdentity("bytes".getBytes(), "image/jpeg"));

        assertThat(error.isReintentable()).isFalse();
    }

    @Test
    void verificarIdentidad_nvidiaResponde429_esReintentable() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        AiServiceUnavailableException error = org.junit.jupiter.api.Assertions.assertThrows(
                AiServiceUnavailableException.class,
                () -> servicio.verifyIdentity("bytes".getBytes(), "image/jpeg"));

        assertThat(error.isReintentable()).isTrue();
    }

    @Test
    void verificarIdentidad_nvidiaResponde413_noEsReintentable() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.PAYLOAD_TOO_LARGE));

        AiServiceUnavailableException error = org.junit.jupiter.api.Assertions.assertThrows(
                AiServiceUnavailableException.class,
                () -> servicio.verifyIdentity("bytes".getBytes(), "image/jpeg"));

        assertThat(error.isReintentable()).isFalse();
    }

    @Test
    void verificarIdentidad_campoNuloEnJson_noProduceLaCadenaNull() throws Exception {
        String contenidoIa = "{\"es_documento_valido\": true, \"nombre_detectado\": null, \"confianza\": 0.8}";
        String respuestaNvidia = new ObjectMapper().writeValueAsString(
                Map.of("choices", java.util.List.of(
                        Map.of("message", Map.of("content", contenidoIa)))));

        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withSuccess(respuestaNvidia, MediaType.APPLICATION_JSON));

        AiVerificationResponse resultado = servicio.verifyIdentity("bytes".getBytes(), "image/jpeg");

        assertThat(resultado.getNombreDetectado()).isNull();
    }

    @Test
    void moderarContenido_nvidiaResponde500_caeEnFallbackSilencioso() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withServerError());

        var resultado = servicio.moderarContenido("hola");

        assertThat(resultado.isEsApropiado()).isTrue();
        assertThat(resultado.getConfianza()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
    }

    @Test
    void constructor_apiKeyVacia_lanzaIllegalStateException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build();
        AiProperties propiedades = new AiProperties();
        propiedades.getNvidia().setApiKey("");

        assertThrows(IllegalStateException.class,
                () -> new NvidiaAiService(builder.build(), propiedades, new tools.jackson.databind.ObjectMapper()));
    }

    private static String respuestaConContenido(String contenido) throws Exception {
        return new ObjectMapper().writeValueAsString(
                Map.of("choices", List.of(Map.of("message", Map.of("content", contenido)))));
    }

    @Test
    void moderarContenido_respuestaValida_parseaResultado() throws Exception {
        String contenido = new ObjectMapper().writeValueAsString(Map.of(
                "es_apropiado", false, "categoria_infraccion", "spam", "confianza", 0.7));
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withSuccess(respuestaConContenido(contenido), MediaType.APPLICATION_JSON));

        IaModeracionResponse resultado = servicio.moderarContenido("mensaje sospechoso");

        assertThat(resultado.isEsApropiado()).isFalse();
        assertThat(resultado.getCategoriaInfraccion()).isEqualTo("spam");
    }

    @Test
    void moderarContenido_nvidiaResponde429YLuego200_reintentaYObtieneResultado() throws Exception {
        String contenido = new ObjectMapper().writeValueAsString(Map.of("es_apropiado", true));
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withSuccess(respuestaConContenido(contenido), MediaType.APPLICATION_JSON));

        IaModeracionResponse resultado = servicio.moderarContenido("hola");

        assertThat(resultado.isEsApropiado()).isTrue();
    }

    @Test
    void clasificarServicio_respuestaValida_parseaEtiquetas() throws Exception {
        String contenido = new ObjectMapper().writeValueAsString(Map.of(
                "categoria_sugerida", "Diseño", "subcategoria_sugerida", "Logos",
                "etiquetas_sugeridas", List.of("moderno", "minimalista"), "confianza", 0.6));
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withSuccess(respuestaConContenido(contenido), MediaType.APPLICATION_JSON));

        AiClassificationResponse resultado = servicio.classifyOffering(
                "Logo", "Un logo minimalista", List.of("Diseño", "Ilustración"));

        assertThat(resultado.getCategoriaSugerida()).isEqualTo("Diseño");
        assertThat(resultado.getEtiquetasSugeridas()).containsExactly("moderno", "minimalista");
    }

    @Test
    void clasificarServicio_nvidiaResponde500_caeEnFallbackSilencioso() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withServerError());

        AiClassificationResponse resultado = servicio.classifyOffering("Logo", "Descripción", List.of("Diseño"));

        assertThat(resultado.getCategoriaSugerida()).isEqualTo("Sin categoría");
    }

    @Test
    void sugerirPreguntasBriefing_respuestaConPreguntas_lasDevuelve() throws Exception {
        String contenido = new ObjectMapper().writeValueAsString(
                Map.of("preguntas", List.of("¿Colores preferidos?", "¿Fecha límite?")));
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withSuccess(respuestaConContenido(contenido), MediaType.APPLICATION_JSON));

        List<String> preguntas = servicio.sugerirPreguntasBriefing("Diseño", "Logo", "Descripción");

        assertThat(preguntas).containsExactly("¿Colores preferidos?", "¿Fecha límite?");
    }

    @Test
    void sugerirPreguntasBriefing_respuestaSinPreguntas_devuelveDefault() throws Exception {
        String contenido = new ObjectMapper().writeValueAsString(Map.of("preguntas", List.of()));
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withSuccess(respuestaConContenido(contenido), MediaType.APPLICATION_JSON));

        List<String> preguntas = servicio.sugerirPreguntasBriefing("Diseño", "Logo", "Descripción");

        assertThat(preguntas).containsExactly("¿Qué necesitas?");
    }

    @Test
    void sugerirPreguntasBriefing_nvidiaResponde500_caeEnFallbackConTresPreguntas() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withServerError());

        List<String> preguntas = servicio.sugerirPreguntasBriefing("Diseño", "Logo", "Descripción");

        assertThat(preguntas).hasSize(3);
    }

    @Test
    void analizarResena_respuestaValida_parseaSentimiento() throws Exception {
        String contenido = new ObjectMapper().writeValueAsString(Map.of(
                "sentimiento", "positivo", "es_coherente_con_estrellas", true,
                "es_spam", false, "es_inapropiado", false, "confianza", 0.9));
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withSuccess(respuestaConContenido(contenido), MediaType.APPLICATION_JSON));

        AiReviewResponse resultado = servicio.analyzeReview("Excelente trabajo", 5);

        assertThat(resultado.getSentimiento()).isEqualTo("positivo");
    }

    @Test
    void analizarResena_nvidiaResponde500_caeEnFallbackNeutro() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withServerError());

        AiReviewResponse resultado = servicio.analyzeReview("texto", 3);

        assertThat(resultado.getSentimiento()).isEqualTo("neutro");
    }

    @Test
    void analizarCertificado_respuestaValida_parseaDictamen() throws Exception {
        String contenido = new ObjectMapper().writeValueAsString(Map.of(
                "es_certificado_valido", true, "confianza", 0.8,
                "institucion_emisora", "Universidad X", "campo_estudio", "Diseño"));
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withSuccess(respuestaConContenido(contenido), MediaType.APPLICATION_JSON));

        AiVerificationResponse resultado = servicio.analyzeCertificate("bytes".getBytes(), "image/jpeg");

        assertThat(resultado.isAprobado()).isTrue();
        assertThat(resultado.getInstitucionEmisora()).isEqualTo("Universidad X");
    }

    @Test
    void verificarIdentidad_nvidiaResponde400_lanzaExcepcionDeCliente() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(AiServiceUnavailableException.class,
                () -> servicio.verifyIdentity("bytes".getBytes(), "image/jpeg"));
    }
}
