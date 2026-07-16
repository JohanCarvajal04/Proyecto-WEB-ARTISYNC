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

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getMode() {
        return mode;
    }

    public String getWebhookId() {
        return webhookId;
    }

    public String getBaseUrl() {
        return "sandbox".equalsIgnoreCase(mode)
                ? "https://api-m.sandbox.paypal.com"
                : "https://api-m.paypal.com";
    }
}
