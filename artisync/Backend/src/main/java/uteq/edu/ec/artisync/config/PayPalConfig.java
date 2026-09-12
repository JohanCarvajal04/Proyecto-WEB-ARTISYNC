package uteq.edu.ec.artisync.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de PayPal para integración con API REST Orders v2.
 * Las credenciales se leen exclusivamente de variables de entorno (RNF-14).
 */
@Configuration
public class PayPalConfig {

    @Value("${paypal.client-id:sandbox_client_id}")
    private String clientId;

    @Value("${paypal.client-secret:sandbox_client_secret}")
    private String clientSecret;

    @Value("${paypal.mode:sandbox}")
    private String mode;

    @Value("${paypal.webhook-id:webhook_id}")
    private String webhookId;

    /**
     * @return el Client ID de la app de PayPal (variable de entorno {@code paypal.client-id})
     */
    public String getClientId() {
        return clientId;
    }

    /**
     * @return el Client Secret de la app de PayPal (variable de entorno {@code paypal.client-secret})
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     * @return {@code "sandbox"} o {@code "live"}, según {@code paypal.mode}; determina la URL base
     */
    public String getMode() {
        return mode;
    }

    /**
     * @return el identificador del webhook de PayPal, usado para verificar la
     *         firma de los eventos entrantes
     */
    public String getWebhookId() {
        return webhookId;
    }

    /**
     * @return la URL base de la API de PayPal Orders v2 que corresponde al {@link #mode} configurado
     */
    public String getBaseUrl() {
        return "sandbox".equalsIgnoreCase(mode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }
}
