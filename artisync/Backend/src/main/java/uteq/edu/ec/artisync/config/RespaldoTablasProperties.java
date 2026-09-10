package uteq.edu.ec.artisync.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mapeo tabla -> columna de fecha para el respaldo INCREMENTAL (prefijo
 * "respaldo.tablas.*"). "incluidas" es la allowlist de tablas en alcance;
 * "columnaFecha" mapea cada una a su columna de auditoría de fecha
 * (actualizado_en, fecha_creacion, ...). Una tabla en "incluidas" sin entrada
 * en "columnaFecha" se exporta completa en cada corrida incremental (no se
 * omite) — ver IncrementalRespaldoExportador.
 *
 * Este mapeo debe poblarse revisando docs/diagramas/Entidad_Relacion.md y las
 * migraciones Flyway para confirmar qué tablas realmente tienen una columna
 * de fecha de modificación; los valores en application.properties son solo
 * ejemplo.
 */
@Data
@Component
@ConfigurationProperties(prefix = "respaldo.tablas")
public class RespaldoTablasProperties {

    private List<String> incluidas = new ArrayList<>();

    private Map<String, String> columnaFecha = new LinkedHashMap<>();
}
