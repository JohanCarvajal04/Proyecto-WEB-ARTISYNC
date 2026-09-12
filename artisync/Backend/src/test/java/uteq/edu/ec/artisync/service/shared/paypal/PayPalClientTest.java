package uteq.edu.ec.artisync.service.shared.paypal;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;
import uteq.edu.ec.artisync.exception.BusinessRuleException;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Prueba de caracterización de PayPalClient, escrita ANTES de renombrar
 * callPayPal/callPayPalIdempotent/getAccessToken (clase en 10,7%
 * líneas / 0% ramas: hoy solo se ejercita mockeada desde quien la consume,
 * nunca su lógica interna real). El campo `restTemplate` es a propósito no
 * `final`, según su propio Javadoc, justamente para poder sustituirlo aquí
 * por un MockRestServiceServer sin salir a la red real.
 */
class PayPalClientTest {

    private static final String TOKEN_RESPONSE = "{\"access_token\":\"token-de-prueba\",\"token_type\":\"Bearer\"}";

    private PayPalClient client;
    private MockRestServiceServer servidor;

    @BeforeEach
    void setUp() {
        client = new PayPalClient();
        RestTemplate restTemplate = new RestTemplate();
        ReflectionTestUtils.setField(client, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(client, "paypalClientId", "id-de-prueba");
        ReflectionTestUtils.setField(client, "paypalClientSecret", "secreto-de-prueba");
        // Fuera de un contexto Spring, @Value("${paypal.mode:sandbox}") no aplica su
        // valor por defecto: el campo queda null salvo que se fije explícitamente aquí.
        ReflectionTestUtils.setField(client, "paypalMode", "sandbox");
        servidor = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    void getPayPalBaseUrl_PorDefectoEnSandbox_DevuelveUrlDeSandbox() {
        assertThat(client.getPayPalBaseUrl()).isEqualTo("https://api-m.sandbox.paypal.com");
    }

    @Test
    void getPayPalBaseUrl_EnModoLive_DevuelveUrlDeProduccion() {
        ReflectionTestUtils.setField(client, "paypalMode", "live");

        assertThat(client.getPayPalBaseUrl()).isEqualTo("https://api-m.paypal.com");
    }

    @Test
    void llamarPayPal_ObtieneTokenYEjecutaLaLlamadaConElBearer() {
        String credencialesEsperadas = Base64.getEncoder().encodeToString("id-de-prueba:secreto-de-prueba".getBytes());
        servidor.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic " + credencialesEsperadas))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://api-m.sandbox.paypal.com/v1/checkout/orders/ORD-1"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andRespond(withSuccess("{\"status\":\"COMPLETED\"}", MediaType.APPLICATION_JSON));

        JsonNode resultado = client.callPayPal("/v1/checkout/orders/ORD-1", HttpMethod.GET, null);

        assertThat(resultado.get("status").asText()).isEqualTo("COMPLETED");
        servidor.verify();
    }

    @Test
    void llamarPayPal_SinIdempotencyKey_NoEnviaLaCabeceraDeIdempotencia() {
        servidor.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://api-m.sandbox.paypal.com/v1/checkout/orders"))
                .andExpect(request -> assertThat(request.getHeaders().get("PayPal-Request-Id")).isNull())
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.callPayPal("/v1/checkout/orders", HttpMethod.POST, null);

        servidor.verify();
    }

    @Test
    void llamarPayPalIdempotente_EnviaLaCabeceraPayPalRequestId() {
        servidor.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://api-m.sandbox.paypal.com/v1/payments/refund"))
                .andExpect(header("PayPal-Request-Id", "clave-idempotencia-1"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.callPayPalIdempotent("/v1/payments/refund", HttpMethod.POST, null, "clave-idempotencia-1");

        servidor.verify();
    }

    @Test
    void llamarPayPal_RespuestaConCuerpoIlegible_LanzaBusinessRuleException() {
        servidor.expect(requestTo("https://api-m.sandbox.paypal.com/v1/oauth2/token"))
                .andRespond(withSuccess(TOKEN_RESPONSE, MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://api-m.sandbox.paypal.com/v1/checkout/orders/ORD-2"))
                .andRespond(withSuccess("esto no es json valido", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> client.callPayPal("/v1/checkout/orders/ORD-2", HttpMethod.GET, null))
                .isInstanceOf(BusinessRuleException.class);
    }
}
