package uteq.edu.ec.artisync.service.shared.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uteq.edu.ec.artisync.config.IaProperties;
import uteq.edu.ec.artisync.dto.ia.IaVerificacionResponse;
import uteq.edu.ec.artisync.exception.ExcepcionServicioIaNoDisponible;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeminiIaServiceTest {

    private MockRestServiceServer servidorSimulado;
    private GeminiIaService servicio;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidorSimulado = MockRestServiceServer.bindTo(builder).build();

        IaProperties propiedades = new IaProperties();
        propiedades.getGemini().setApiKey("gemini-test-key");
        propiedades.getGemini().setBaseUrl("https://generativelanguage.googleapis.com/v1beta");
        propiedades.getGemini().setModel("gemini-2.0-flash");

        servicio = new GeminiIaService(builder.build(), propiedades, new tools.jackson.databind.ObjectMapper());
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

        IaVerificacionResponse resultado = servicio.verificarIdentidad("bytes".getBytes(), "image/jpeg");

        assertThat(resultado.isAprobado()).isTrue();
    }

    @Test
    void verificarIdentidad_geminiResponde500_lanzaExcepcionServicioNoDisponible() {
        servidorSimulado.expect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withServerError());

        assertThrows(ExcepcionServicioIaNoDisponible.class,
                () -> servicio.verificarIdentidad("bytes".getBytes(), "image/jpeg"));
    }
}
