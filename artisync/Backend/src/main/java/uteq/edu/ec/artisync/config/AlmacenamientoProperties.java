package uteq.edu.ec.artisync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de almacenamiento de documentos, prefijo "documentos.*".
 * "proveedor" elige qué implementación de AlmacenamientoDocumentos se registra:
 * "local" (volumen del contenedor, por defecto y en CI) o "azure" (Blob Storage).
 */
@Data
@Component
@ConfigurationProperties(prefix = "documentos")
public class AlmacenamientoProperties {

    private String proveedor = "local";
    private String rutaBase = "/var/artisync/documentos";
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
