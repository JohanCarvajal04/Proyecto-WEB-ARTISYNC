package uteq.edu.ec.artisync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Propiedades de almacenamiento de documentos, prefijo "documentos.*".
 *
 * <p>Los dos backends conviven: AlmacenamientoRouter decide por prefijo cuál
 * usa cada archivo. Ya no hay un "proveedor" que elija uno u otro — ver ADR-007.
 */
@Data
@Component
@ConfigurationProperties(prefix = "documentos")
public class AlmacenamientoProperties {

    private String rutaBase = "/var/artisync/documentos";

    /**
     * Prefijos que se guardan en el volumen local en lugar de Azure. Por defecto
     * solo verificación: son cédulas y títulos que caducan a los 30 días
     * (VerificacionScheduler), así que no compensa subirlos a la nube. Todo lo
     * que no esté aquí va a Azure.
     */
    private List<String> prefijosLocales = List.of("verificacion");

    private Azure azure = new Azure();

    @Data
    public static class Azure {

        /** Cadena de conexión de la cuenta de almacenamiento. Nunca se versiona: llega por variable de entorno. */
        private String connectionString = "";

        /** Contenedor destino. Debe ser privado: guarda cédulas y títulos. */
        private String contenedor = "artisync-documentos";

        /** Vigencia de las URLs temporales (SAS) que se entregan al frontend. */
        private long sasMinutos = 15;
    }
}
