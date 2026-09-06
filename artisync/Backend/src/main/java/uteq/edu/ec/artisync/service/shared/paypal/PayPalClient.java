package uteq.edu.ec.artisync.service.shared.paypal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;

import java.util.Base64;
import java.util.Map;

/**
 * Cliente HTTP para la API REST de PayPal (checkout/orders y payouts),
 * compartido por PagoServicioImpl y SolicitudRetiroServicioImpl. Antes estaba
 * duplicado palabra por palabra entre ambos.
 */
@Component
public class PayPalClient {

    /**
     * Mapper propio, no el del contexto: el contrato con PayPal no debe verse
     * afectado por personalizaciones de serialización de la aplicación.
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * No es `final` para que las pruebas puedan sustituirlo: el camino de
     * verificación y captura es el que mueve dinero y conviene poder ejercitarlo
     * sin salir a la red.
     */
    private RestTemplate restTemplate = new RestTemplate();

    @Value("${paypal.client-id:sandbox_client_id}")
    private String paypalClientId;

    @Value("${paypal.client-secret:sandbox_client_secret}")
    private String paypalClientSecret;

    @Value("${paypal.mode:sandbox}")
    private String paypalMode;

    public String getPayPalBaseUrl() {
        return "sandbox".equalsIgnoreCase(paypalMode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }

    /** Llamada autenticada a la API de PayPal. `cuerpo` null para GET. */
    public JsonNode llamarPayPal(String ruta, HttpMethod metodo, JsonNode cuerpo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(obtenerAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> peticion = new HttpEntity<>(cuerpo != null ? cuerpo.toString() : null, headers);

        ResponseEntity<String> respuesta = restTemplate.exchange(
                getPayPalBaseUrl() + ruta, metodo, peticion, String.class);

        try {
            return objectMapper.readTree(respuesta.getBody());
        } catch (Exception e) {
            throw new ExcepcionReglaNegocio("Respuesta ilegible de PayPal en " + ruta);
        }
    }

    private String obtenerAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        String credentials = Base64.getEncoder().encodeToString(
                (paypalClientId + ":" + paypalClientSecret).getBytes());
        headers.set("Authorization", "Basic " + credentials);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<String> request = new HttpEntity<>("grant_type=client_credentials", headers);

        ResponseEntity<Map> response = restTemplate.exchange(
                getPayPalBaseUrl() + "/v1/oauth2/token",
                HttpMethod.POST, request, Map.class);

        return (String) response.getBody().get("access_token");
    }
}
