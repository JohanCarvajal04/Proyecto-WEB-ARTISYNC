package uteq.edu.ec.artisync.service.respaldo.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.config.BackupProperties;
import uteq.edu.ec.artisync.entity.respaldo.Backup;
import uteq.edu.ec.artisync.service.shared.reporte.ValueFormatter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Ejecuta un respaldo FULL vía el binario pg_dump (ProcessBuilder), conectado
 * por red al host de Postgres con el rol de solo lectura artisync_backup.
 *
 * A propósito NUNCA "docker exec" al contenedor sibling de postgres: eso
 * requeriría montar el socket de Docker en el contenedor backend (control
 * total del host Docker), una escalada de privilegios que contradice el
 * resto de la postura de mínimo privilegio del proyecto. Conectar por red
 * además es portable a Render/Azure (Postgres remoto gestionado).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PgDumpExecutor {

    private static final DateTimeFormatter MARCA_TIEMPO = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private final BackupProperties respaldoProperties;
    private final BackupFileStorage storage;

    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param respaldo parametro requerido para la correcta ejecucion del procedimiento
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Path execute(Backup respaldo) throws IOException, InterruptedException {
        BackupProperties.Db db = respaldoProperties.getDb();
        String nombreArchivo = "respaldo_full_" + LocalDateTime.now(ValueFormatter.zona()).format(MARCA_TIEMPO) + ".dump";
        Path destino = storage.resolveDestinationPath(nombreArchivo);
        Path logError = storage.resolveDestinationPath(nombreArchivo + ".stderr.log");

        List<String> comando = buildCommand(db, destino);

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.environment().put("PGPASSWORD", db.getPassword());
        // -f ya escribe el dump a disco (stdout vacío); solo stderr lleva
        // progreso/errores de pg_dump. Redirigir a archivo, NUNCA leer los
        // streams del hijo a mano mientras se bloquea en waitFor(): es el
        // deadlock clásico de ProcessBuilder si el hijo llena el buffer.
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(logError.toFile());

        Process proceso = pb.start();
        boolean termino = proceso.waitFor(30, TimeUnit.MINUTES);
        if (!termino) {
            proceso.destroyForcibly();
            throw new IllegalStateException("pg_dump excedió el tiempo máximo de 30 minutos");
        }
        if (proceso.exitValue() != 0) {
            String detalle = Files.exists(logError) ? Files.readString(logError) : "(sin detalle de stderr)";
            throw new IllegalStateException("pg_dump falló (código " + proceso.exitValue() + "): " + detalle);
        }

        Files.deleteIfExists(logError);
        return destino;
    }

    /** Extraído para poder probar la construcción del comando sin invocar pg_dump real. */
    List<String> buildCommand(BackupProperties.Db db, Path destino) {
        return List.of(
                "pg_dump",
                "-h", db.getHost(),
                "-p", String.valueOf(db.getPuerto()),
                "-U", db.getUsuario(),
                "-Fc",
                "-f", destino.toString(),
                db.getNombre());
    }
}
