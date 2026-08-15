package uteq.edu.ec.artisync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "documentos")
public class AlmacenamientoProperties {
    private String rutaBase = "/var/artisync/documentos";
}
