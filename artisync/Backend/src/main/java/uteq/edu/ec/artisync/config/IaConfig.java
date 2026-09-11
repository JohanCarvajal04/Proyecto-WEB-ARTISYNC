package uteq.edu.ec.artisync.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Componente de Infraestructura: Configuracion de Integracion IA.
 * 
 * Propósito: Configurar los beans y clientes HTTP necesarios para conectarse a las APIs de Inteligencia Artificial.
 * 
 * Flujo interno: Se carga al inicio del contexto de Spring y provee instancias singletons configuradas para los servicios de IA.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class IaConfig {

    private final AiProperties iaProperties;

    @Bean("iaRestClient")
    public RestClient iaRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        Duration timeout = Duration.ofSeconds(iaProperties.getTimeoutSeconds());
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);

        log.info("RestClient de IA configurado [proveedor={}, timeout={}s]",
                iaProperties.getProvider(), iaProperties.getTimeoutSeconds());

        return RestClient.builder().requestFactory(factory).build();
    }
}

