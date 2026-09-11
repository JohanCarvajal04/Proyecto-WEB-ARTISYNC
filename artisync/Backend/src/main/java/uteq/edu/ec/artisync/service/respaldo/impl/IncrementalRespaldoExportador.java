package uteq.edu.ec.artisync.service.respaldo.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.config.RespaldoTablasProperties;
import uteq.edu.ec.artisync.entity.respaldo.Respaldo;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Ejecuta un respaldo INCREMENTAL: por cada tabla en alcance, corre
 * COPY (...) TO STDOUT vía el CopyManager del driver JDBC de PostgreSQL
 * (org.postgresql.copy), NO el binario pg_dump — pg_dump no admite filtrar
 * filas por WHERE, así que un incremental "vía pg_dump filtrado" no es
 * posible; este es el mecanismo real.
 *
 * Tablas sin columna de fecha reconocida se exportan COMPLETAS (fallback),
 * no se omiten. Limitación a tener presente (documentada en
 * docs/despliegue/BACKUP.md): no es un incremental de recuperación ante
 * desastres en sentido estricto — no reconstruye el estado completo sin el
 * último FULL, y no captura DELETEs de forma confiable.
 */
@Component
@RequiredArgsConstructor
public class IncrementalRespaldoExportador {

    private static final DateTimeFormatter MARCA_TIEMPO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");
    private static final DateTimeFormatter CORTE_SQL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS");

    private final @Qualifier("respaldoDataSource") DataSource respaldoDataSource;
    private final RespaldoTablasProperties tablasProperties;
    private final RespaldoArchivoStorage storage;
    private final ObjectMapper objectMapper;

    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param respaldo parametro requerido para la correcta ejecucion del procedimiento
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Path ejecutar(Respaldo respaldo) throws IOException, SQLException {
        String nombreArchivo = "respaldo_incremental_" + LocalDateTime.now().format(MARCA_TIEMPO) + ".zip";
        Path destino = storage.resolverRutaDestino(nombreArchivo);
        List<ManifiestoTabla> manifiestoTablas = new ArrayList<>();

        try (Connection conexion = respaldoDataSource.getConnection();
             ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(destino))) {

            CopyManager copyManager = conexion.unwrap(PGConnection.class).getCopyAPI();

            for (String tabla : tablasProperties.getIncluidas()) {
                String columnaFecha = tablasProperties.getColumnaFecha().get(tabla);
                String sql = construirSentenciaCopy(tabla, columnaFecha, respaldo.getFechaDesdeIncremental());

                zip.putNextEntry(new ZipEntry(tabla + ".csv"));
                long filas = copyManager.copyOut(sql, new FlujoSinCierre(zip));
                zip.closeEntry();

                manifiestoTablas.add(new ManifiestoTabla(tabla, columnaFecha, filas));
            }

            ManifiestoIncremental manifiesto = new ManifiestoIncremental(
                    respaldo.getIdRespaldoFullBase(),
                    LocalDateTime.now().format(CORTE_SQL),
                    respaldo.getFechaDesdeIncremental() != null ? respaldo.getFechaDesdeIncremental().format(CORTE_SQL) : null,
                    manifiestoTablas);
            zip.putNextEntry(new ZipEntry("manifiesto.json"));
            zip.write(objectMapper.writeValueAsBytes(manifiesto));
            zip.closeEntry();
        }
        return destino;
    }

    /**
     * Nombres de tabla/columna vienen únicamente de RespaldoTablasProperties
     * (configuración propia, nunca de entrada HTTP), y el corte de fecha es
     * un LocalDateTime calculado internamente por el servicio — no hay
     * entrada no confiable en esta cadena de texto.
     */
    private String construirSentenciaCopy(String tabla, String columnaFecha, LocalDateTime corte) {
        if (columnaFecha == null || corte == null) {
            return "COPY %s TO STDOUT WITH (FORMAT csv, HEADER true)".formatted(tabla);
        }
        return "COPY (SELECT * FROM %s WHERE %s > '%s') TO STDOUT WITH (FORMAT csv, HEADER true)"
                .formatted(tabla, columnaFecha, corte.format(CORTE_SQL));
    }

    /** OutputStream que no cierra el ZipOutputStream subyacente al terminar cada COPY. */
    private static final class FlujoSinCierre extends OutputStream {
        private final OutputStream delegado;

        FlujoSinCierre(OutputStream delegado) {
            this.delegado = delegado;
        }

        @Override
        /**
         * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
         *
         * @param b parametro requerido para la correcta ejecucion del procedimiento
         * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
         */
        public void write(int b) throws IOException {
            delegado.write(b);
        }

        @Override
        /**
         * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
         *
         * @param b parametro requerido para la correcta ejecucion del procedimiento
         * @param off parametro requerido para la correcta ejecucion del procedimiento
         * @param len parametro requerido para la correcta ejecucion del procedimiento
         * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
         */
        public void write(byte[] b, int off, int len) throws IOException {
            delegado.write(b, off, len);
        }

        @Override
        /**
         * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
         *
         * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
         */
        public void flush() throws IOException {
            delegado.flush();
        }

        @Override
        /**
         * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
         *
         * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
         */
        public void close() {
            // A propósito no cierra: el ZipOutputStream sigue abierto para la
            // siguiente entrada/tabla.
        }
    }

    record ManifiestoTabla(String tabla, String columnaFecha, long filas) {
    }

    /**
     * fechaExportacion/fechaDesde van como String preformateado (no
     * LocalDateTime): el ObjectMapper inyectado es el bean simple de
     * ArtisyncApplication (sin el módulo jackson-datatype-jsr310), y
     * serializar un LocalDateTime directo falla en tiempo de ejecución. Se
     * evita el problema en vez de tocar ese bean compartido por toda la app.
     */
    record ManifiestoIncremental(Long idRespaldoFullBase, String fechaExportacion,
                                  String fechaDesde, List<ManifiestoTabla> tablas) {
    }
}
