package uteq.edu.ec.artisync.service.respaldo.impl;

import org.junit.jupiter.api.Test;
import uteq.edu.ec.artisync.config.BackupProperties;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de caracterización de PgDumpExecutor, escrita ANTES de renombrar `execute`.
 * La ejecución real del binario pg_dump no se puede probar sin Postgres/pg_dump
 * instalados (fuera de este entorno) — este test cubre la parte determinista y
 * testeable sin invocar el proceso real: la construcción de los argumentos del
 * comando, extraída a buildCommand(...) específicamente para esto.
 */
class PgDumpExecutorTest {

    @Test
    void construirComando_ArmaLosFlagsDePgDumpConLosDatosDeConexion() {
        PgDumpExecutor ejecutor = new PgDumpExecutor(new BackupProperties(), null);
        BackupProperties.Db db = new BackupProperties.Db();
        db.setHost("db.interno");
        db.setPuerto(5433);
        db.setUsuario("artisync_backup");
        db.setNombre("artisyncbd_prod");
        Path destino = Path.of("/tmp/respaldos/full.dump");

        List<String> comando = ejecutor.buildCommand(db, destino);

        assertThat(comando).containsExactly(
                "pg_dump",
                "-h", "db.interno",
                "-p", "5433",
                "-U", "artisync_backup",
                "-Fc",
                "-f", destino.toString(),
                "artisyncbd_prod");
    }
}
