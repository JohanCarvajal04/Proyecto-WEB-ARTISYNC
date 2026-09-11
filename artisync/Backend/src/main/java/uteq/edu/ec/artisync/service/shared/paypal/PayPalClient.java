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

    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public String getPayPalBaseUrl() {
        return "sandbox".equalsIgnoreCase(paypalMode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }

    /** Llamada autenticada a la API de PayPal. `cuerpo` null para GET. */
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param ruta parametro requerido para la correcta ejecucion del procedimiento
     * @param metodo parametro requerido para la correcta ejecucion del procedimiento
     * @param cuerpo parametro requerido para la correcta ejecucion del procedimiento
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public JsonNode llamarPayPal(String ruta, HttpMethod metodo, JsonNode cuerpo) {
        return llamarPayPal(ruta, metodo, cuerpo, null);
    }

    /**
     * Igual que {@link #llamarPayPal(String, HttpMethod, JsonNode)}, pero con
     * cabecera `PayPal-Request-Id` (REQ-NF-019): un reembolso reintentado con
     * la misma clave no se procesa dos veces del lado de PayPal, mismo
     * principio que el `sender_batch_id` idempotente de los payouts en
     * SolicitudRetiroServicioImpl, aplicado aquí vía el mecanismo propio que
     * PayPal expone para checkout/orders y captures/refund.
     */
    public JsonNode llamarPayPalIdempotente(String ruta, HttpMethod metodo, JsonNode cuerpo, String idempotencyKey) {
        return llamarPayPal(ruta, metodo, cuerpo, idempotencyKey);
    }

    private JsonNode llamarPayPal(String ruta, HttpMethod metodo, JsonNode cuerpo, String idempotencyKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(obtenerAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            headers.set("PayPal-Request-Id", idempotencyKey);
        }

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
