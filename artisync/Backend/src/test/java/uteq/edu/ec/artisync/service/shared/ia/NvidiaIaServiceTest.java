package uteq.edu.ec.artisync.service.shared.ia;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import uteq.edu.ec.artisync.config.IaProperties;
import uteq.edu.ec.artisync.dto.ia.IaVerificacionResponse;
import uteq.edu.ec.artisync.exception.ExcepcionServicioIaNoDisponible;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class NvidiaIaServiceTest {

    private MockRestServiceServer servidorSimulado;
    private NvidiaIaService servicio;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidorSimulado = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        IaProperties propiedades = new IaProperties();
        propiedades.getNvidia().setApiKey("nvapi-test-key");
        propiedades.getNvidia().setBaseUrl("https://integrate.api.nvidia.com/v1");

        servicio = new NvidiaIaService(restClient, propiedades, new tools.jackson.databind.ObjectMapper());
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

        IaVerificacionResponse resultado = servicio.verificarIdentidad("contenido-imagen".getBytes(), "image/jpeg");

        assertThat(resultado.isAprobado()).isTrue();
        assertThat(resultado.getNombreDetectado()).isEqualTo("Ana Pérez");
        assertThat(resultado.getConfianza()).isEqualByComparingTo("0.91");
    }

    @Test
    void verificarIdentidad_nvidiaResponde500_lanzaExcepcionServicioNoDisponible() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withServerError());

        assertThrows(ExcepcionServicioIaNoDisponible.class,
                () -> servicio.verificarIdentidad("bytes".getBytes(), "image/jpeg"));
    }

    @Test
    void verificarIdentidad_nvidiaResponde401_lanzaExcepcionServicioNoDisponible() {
        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(ExcepcionServicioIaNoDisponible.class,
                () -> servicio.verificarIdentidad("bytes".getBytes(), "image/jpeg"));
    }

    @Test
    void verificarIdentidad_campoNuloEnJson_noProduceLaCadenaNull() throws Exception {
        String contenidoIa = "{\"es_documento_valido\": true, \"nombre_detectado\": null, \"confianza\": 0.8}";
        String respuestaNvidia = new ObjectMapper().writeValueAsString(
                Map.of("choices", java.util.List.of(
                        Map.of("message", Map.of("content", contenidoIa)))));

        servidorSimulado.expect(requestTo("https://integrate.api.nvidia.com/v1/chat/completions"))
                .andRespond(withSuccess(respuestaNvidia, MediaType.APPLICATION_JSON));

        IaVerificacionResponse resultado = servicio.verificarIdentidad("bytes".getBytes(), "image/jpeg");

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
        IaProperties propiedades = new IaProperties();
        propiedades.getNvidia().setApiKey("");

        assertThrows(IllegalStateException.class,
                () -> new NvidiaIaService(builder.build(), propiedades, new tools.jackson.databind.ObjectMapper()));
    }
}
