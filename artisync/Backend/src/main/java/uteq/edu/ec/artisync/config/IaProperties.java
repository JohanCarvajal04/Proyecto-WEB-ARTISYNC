package uteq.edu.ec.artisync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades tipadas de IA, prefijo "ia.*". La IA solo asiste al moderador
 * (ver docs/superpowers/specs/2026-08-06-ia-verificacion-asistida-design.md);
 * confidenceThreshold marca un dictamen como poco fiable, no aprueba nada.
 */
@Data
@Component
@ConfigurationProperties(prefix = "ia")
public class IaProperties {

    private String provider = "mock";
    private double confidenceThreshold = 0.75;
    private int timeoutSeconds = 30;
    private GeminiConfig gemini = new GeminiConfig();
    private NvidiaConfig nvidia = new NvidiaConfig();

    @Data
    public static class GeminiConfig {
        private String apiKey = "";
        private String model = "gemini-2.0-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    }

    @Data
    public static class NvidiaConfig {
        private String apiKey = "";
        private String model = "nvidia/llama-3.2-nv-vision-instruct";
        private String baseUrl = "https://integrate.api.nvidia.com/v1";
    }
}
