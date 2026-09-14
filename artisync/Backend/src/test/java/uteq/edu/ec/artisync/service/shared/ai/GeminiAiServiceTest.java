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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiAiServiceTest {

    private MockRestServiceServer servidorSimulado;
    private GeminiAiService servicio;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidorSimulado = MockRestServiceServer.bindTo(builder).build();

        AiProperties propiedades = new AiProperties();
        propiedades.getGemini().setApiKey("gemini-test-key");
        propiedades.getGemini().setBaseUrl("https://generativelanguage.googleapis.com/v1beta");
        propiedades.getGemini().setModel("gemini-2.0-flash");

        servicio = new GeminiAiService(builder.build(), propiedades, new tools.jackson.databind.ObjectMapper());
    }

    @Test
    void verificarIdentidad_respuestaValida_parseaDictamen() throws Exception {
        String texto = new ObjectMapper().writeValueAsString(
                Map.of("es_documento_valido", true, "confianza", 0.85));
        String respuestaGemini = new ObjectMapper().writeValueAsString(Map.of(
                "candidates", List.of(Map.of("content", Map.of(
                        "parts", List.of(Map.of("text", texto)))))));

        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(respuestaGemini, MediaType.APPLICATION_JSON));

        AiVerificationResponse resultado = servicio.verifyIdentity("bytes".getBytes(), "image/jpeg");

        assertThat(resultado.isAprobado()).isTrue();
    }

    @Test
    void verificarIdentidad_geminiResponde500_lanzaExcepcionServicioNoDisponible() {
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(AiServiceUnavailableException.class,
                () -> servicio.verifyIdentity("bytes".getBytes(), "image/jpeg"));
    }

    @Test
    void verificarIdentidad_geminiResponde429_esReintentable() {
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        AiServiceUnavailableException error = assertThrows(AiServiceUnavailableException.class,
                () -> servicio.verifyIdentity("bytes".getBytes(), "image/jpeg"));

        assertThat(error.isReintentable()).isTrue();
    }

    @Test
    void verificarIdentidad_geminiResponde401_noEsReintentable() {
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        AiServiceUnavailableException error = assertThrows(AiServiceUnavailableException.class,
                () -> servicio.verifyIdentity("bytes".getBytes(), "image/jpeg"));

        assertThat(error.isReintentable()).isFalse();
    }

    @Test
    void verificarIdentidad_geminiResponde400_lanzaExcepcionDeCliente() {
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThrows(AiServiceUnavailableException.class,
                () -> servicio.verifyIdentity("bytes".getBytes(), "image/jpeg"));
    }

    private static String respuestaConTexto(String texto) throws Exception {
        return new ObjectMapper().writeValueAsString(Map.of(
                "candidates", List.of(Map.of("content", Map.of(
                        "parts", List.of(Map.of("text", texto)))))));
    }

    @Test
    void moderarContenido_respuestaValida_parseaResultado() throws Exception {
        String texto = new ObjectMapper().writeValueAsString(Map.of(
                "es_apropiado", false, "categoria_infraccion", "spam", "confianza", 0.7));
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(respuestaConTexto(texto), MediaType.APPLICATION_JSON));

        IaModeracionResponse resultado = servicio.moderarContenido("mensaje sospechoso");

        assertThat(resultado.isEsApropiado()).isFalse();
        assertThat(resultado.getCategoriaInfraccion()).isEqualTo("spam");
    }

    @Test
    void moderarContenido_geminiResponde500_caeEnFallbackSilencioso() {
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withServerError());

        IaModeracionResponse resultado = servicio.moderarContenido("hola");

        assertThat(resultado.isEsApropiado()).isTrue();
        assertThat(resultado.getConfianza()).isEqualByComparingTo(java.math.BigDecimal.ZERO);
    }

    @Test
    void moderarContenido_geminiResponde429YLuego200_reintentaYObtieneResultado() throws Exception {
        String texto = new ObjectMapper().writeValueAsString(Map.of("es_apropiado", true));
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(respuestaConTexto(texto), MediaType.APPLICATION_JSON));

        IaModeracionResponse resultado = servicio.moderarContenido("hola");

        assertThat(resultado.isEsApropiado()).isTrue();
    }

    @Test
    void clasificarServicio_respuestaValida_parseaEtiquetas() throws Exception {
        String texto = new ObjectMapper().writeValueAsString(Map.of(
                "categoria_sugerida", "Diseño", "subcategoria_sugerida", "Logos",
                "etiquetas_sugeridas", List.of("moderno", "minimalista"), "confianza", 0.6));
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(respuestaConTexto(texto), MediaType.APPLICATION_JSON));

        AiClassificationResponse resultado = servicio.classifyOffering(
                "Logo", "Un logo minimalista", List.of("Diseño", "Ilustración"));

        assertThat(resultado.getCategoriaSugerida()).isEqualTo("Diseño");
        assertThat(resultado.getEtiquetasSugeridas()).containsExactly("moderno", "minimalista");
    }

    @Test
    void clasificarServicio_geminiResponde500_caeEnFallbackSilencioso() {
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withServerError());

        AiClassificationResponse resultado = servicio.classifyOffering("Logo", "Descripción", List.of("Diseño"));

        assertThat(resultado.getCategoriaSugerida()).isEqualTo("Sin categoría");
    }

    @Test
    void sugerirPreguntasBriefing_respuestaConPreguntas_lasDevuelve() throws Exception {
        String texto = new ObjectMapper().writeValueAsString(
                Map.of("preguntas", List.of("¿Colores preferidos?", "¿Fecha límite?")));
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(respuestaConTexto(texto), MediaType.APPLICATION_JSON));

        List<String> preguntas = servicio.sugerirPreguntasBriefing("Diseño", "Logo", "Descripción");

        assertThat(preguntas).containsExactly("¿Colores preferidos?", "¿Fecha límite?");
    }

    @Test
    void sugerirPreguntasBriefing_respuestaSinPreguntas_devuelveDefault() throws Exception {
        String texto = new ObjectMapper().writeValueAsString(Map.of("preguntas", List.of()));
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(respuestaConTexto(texto), MediaType.APPLICATION_JSON));

        List<String> preguntas = servicio.sugerirPreguntasBriefing("Diseño", "Logo", "Descripción");

        assertThat(preguntas).containsExactly("¿Qué necesitas?");
    }

    @Test
    void sugerirPreguntasBriefing_geminiResponde500_caeEnFallbackConTresPreguntas() {
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withServerError());

        List<String> preguntas = servicio.sugerirPreguntasBriefing("Diseño", "Logo", "Descripción");

        assertThat(preguntas).hasSize(3);
    }

    @Test
    void analizarResena_respuestaValida_parseaSentimiento() throws Exception {
        String texto = new ObjectMapper().writeValueAsString(Map.of(
                "sentimiento", "positivo", "es_coherente_con_estrellas", true,
                "es_spam", false, "es_inapropiado", false, "confianza", 0.9));
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(respuestaConTexto(texto), MediaType.APPLICATION_JSON));

        AiReviewResponse resultado = servicio.analyzeReview("Excelente trabajo", 5);

        assertThat(resultado.getSentimiento()).isEqualTo("positivo");
    }

    @Test
    void analizarResena_geminiResponde500_caeEnFallbackNeutro() {
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withServerError());

        AiReviewResponse resultado = servicio.analyzeReview("texto", 3);

        assertThat(resultado.getSentimiento()).isEqualTo("neutro");
    }

    @Test
    void analizarCertificado_respuestaValida_parseaDictamen() throws Exception {
        String texto = new ObjectMapper().writeValueAsString(Map.of(
                "es_certificado_valido", true, "confianza", 0.8,
                "institucion_emisora", "Universidad X", "campo_estudio", "Diseño"));
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(respuestaConTexto(texto), MediaType.APPLICATION_JSON));

        AiVerificationResponse resultado = servicio.analyzeCertificate("bytes".getBytes(), "image/jpeg");

        assertThat(resultado.isAprobado()).isTrue();
        assertThat(resultado.getInstitucionEmisora()).isEqualTo("Universidad X");
    }
}
