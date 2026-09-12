package uteq.edu.ec.artisync.service.respaldo.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;
import uteq.edu.ec.artisync.config.BackupProperties;
import uteq.edu.ec.artisync.config.BackupTablesProperties;
import uteq.edu.ec.artisync.entity.respaldo.Backup;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba de caracterización de IncrementalBackupExporter, escrita ANTES de renombrar
 * `ejecutar`/`construirSentenciaCopy` (clase en ~7% de cobertura real). No requiere
 * Postgres real: mockea la cadena DataSource -> Connection -> PGConnection ->
 * CopyManager, que es lo único que el método invoca de verdad contra la base.
 */
@ExtendWith(MockitoExtension.class)
class IncrementalBackupExporterTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private PGConnection pgConnection;
    @Mock
    private CopyManager copyManager;

    private IncrementalBackupExporter exportadorSinConexion(Path tempDir, BackupTablesProperties tablas) {
        BackupProperties propiedades = new BackupProperties();
        propiedades.setRutaBase(tempDir.toString());
        BackupFileStorage storage = new BackupFileStorage(propiedades);

        return new IncrementalBackupExporter(dataSource, tablas, storage, new ObjectMapper());
    }

    private IncrementalBackupExporter exportador(Path tempDir, BackupTablesProperties tablas) throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.unwrap(PGConnection.class)).thenReturn(pgConnection);
        when(pgConnection.getCopyAPI()).thenReturn(copyManager);

        return exportadorSinConexion(tempDir, tablas);
    }

    private Backup respaldoIncremental(Long idFullBase, LocalDateTime desde) {
        Backup respaldo = new Backup();
        respaldo.setIdRespaldoFullBase(idFullBase);
        respaldo.setFechaDesdeIncremental(desde);
        return respaldo;
    }

    @Test
    void ejecutar_TablaSinColumnaFechaConfigurada_ExportaCopyCompletoYArmaElZip(@TempDir Path tempDir) throws Exception {
        BackupTablesProperties tablas = new BackupTablesProperties();
        tablas.setIncluidas(List.of("usuarios"));
        tablas.setColumnaFecha(new LinkedHashMap<>());
        IncrementalBackupExporter exportador = exportador(tempDir, tablas);

        when(copyManager.copyOut(anyString(), any(OutputStream.class))).thenReturn(3L);

        Path resultado = exportador.ejecutar(respaldoIncremental(10L, null));

        verify(copyManager).copyOut(
                eq("COPY usuarios TO STDOUT WITH (FORMAT csv, HEADER true)"), any(OutputStream.class));
        try (ZipFile zip = new ZipFile(resultado.toFile())) {
            assertThat(zip.getEntry("usuarios.csv")).isNotNull();
            ZipEntry manifiesto = zip.getEntry("manifiesto.json");
            assertThat(manifiesto).isNotNull();
        }
    }

    @Test
    void ejecutar_ConColumnaFechaYCorte_UsaCopyFiltradoPorFecha(@TempDir Path tempDir) throws Exception {
        BackupTablesProperties tablas = new BackupTablesProperties();
        tablas.setIncluidas(List.of("pedidos"));
        LinkedHashMap<String, String> columnaFecha = new LinkedHashMap<>();
        columnaFecha.put("pedidos", "actualizado_en");
        tablas.setColumnaFecha(columnaFecha);
        IncrementalBackupExporter exportador = exportador(tempDir, tablas);

        when(copyManager.copyOut(anyString(), any(OutputStream.class))).thenReturn(7L);
        LocalDateTime corte = LocalDateTime.of(2026, 1, 1, 0, 0, 0);

        exportador.ejecutar(respaldoIncremental(5L, corte));

        String sqlEsperado = exportador.construirSentenciaCopy("pedidos", "actualizado_en", corte);
        verify(copyManager).copyOut(eq(sqlEsperado), any(OutputStream.class));
    }

    @Test
    void construirSentenciaCopy_SinColumnaFecha_GeneraCopyCompleto(@TempDir Path tempDir) {
        IncrementalBackupExporter exportador = exportadorSinConexion(tempDir, new BackupTablesProperties());

        String sql = exportador.construirSentenciaCopy("usuarios", null, null);

        assertThat(sql).isEqualTo("COPY usuarios TO STDOUT WITH (FORMAT csv, HEADER true)");
    }

    @Test
    void construirSentenciaCopy_ConColumnaFechaYCorte_GeneraCopyFiltrado(@TempDir Path tempDir) {
        IncrementalBackupExporter exportador = exportadorSinConexion(tempDir, new BackupTablesProperties());
        LocalDateTime corte = LocalDateTime.of(2026, 3, 15, 8, 30, 0);

        String sql = exportador.construirSentenciaCopy("pedidos", "actualizado_en", corte);

        assertThat(sql).isEqualTo(
                "COPY (SELECT * FROM pedidos WHERE actualizado_en > '2026-03-15 08:30:00.000000') TO STDOUT WITH (FORMAT csv, HEADER true)");
    }
}
