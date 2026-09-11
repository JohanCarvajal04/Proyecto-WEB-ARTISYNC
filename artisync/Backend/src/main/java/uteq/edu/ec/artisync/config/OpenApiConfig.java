package uteq.edu.ec.artisync.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(title = "ARTISYNC API REST", version = "1.0", description = "Plataforma de conexiÃ³n para creadores artÃ­sticos y clientes"),
    security = @SecurityRequirement(name = "bearerAuth")
)
/**
 * Componente de Infraestructura: Configuracion de Swagger/OpenAPI.
 * 
 * Propósito: Generar la documentacion automatica e interactiva de los endpoints REST de la aplicacion.
 * 
 * Flujo interno: Registra los metadatos globales (titulo, version, esquema de seguridad Bearer JWT) expuestos publicamente en /v3/api-docs.
 */
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)
public class OpenApiConfig {
}

