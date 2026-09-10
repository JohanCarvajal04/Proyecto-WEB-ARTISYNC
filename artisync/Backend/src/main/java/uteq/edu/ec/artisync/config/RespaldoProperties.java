package uteq.edu.ec.artisync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Propiedades de respaldos de base de datos (REQ-NF-024), prefijo "respaldo.*".
 * Mismo criterio que AlmacenamientoProperties ("documentos.*"), pero sin
 * abstracción de proveedor: el alcance actual es solo almacenamiento local.
 */
@Data
@Component
@ConfigurationProperties(prefix = "respaldo")
public class RespaldoProperties {

    private String rutaBase = "/var/artisync/respaldos";

    /** Retención por defecto (días) para respaldos MANUALES sin programación asociada. */
    private int retencionDiasManual = 30;

    private Db db = new Db();

    /**
     * host/puerto/nombre discretos (no se parsea DB_URL): ProcessBuilder
     * necesita argumentos sueltos para el comando pg_dump.
     */
    @Data
    public static class Db {
        private String host = "localhost";
        private int puerto = 5432;
        private String nombre = "artisyncbd";
        /** Rol de SOLO LECTURA dedicado (artisync_backup) — ver db/seed_privilegios.sh. */
        private String usuario = "artisync_backup";
        private String password = "";
    }
}
