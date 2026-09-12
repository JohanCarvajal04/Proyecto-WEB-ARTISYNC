package uteq.edu.ec.artisync.service.respaldo.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uteq.edu.ec.artisync.config.BackupProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba de caracterización de BackupFileStorage, escrita ANTES de renombrar
 * resolveDestinationPath/delete (clase en 0% de cobertura real). Sin mocks: opera
 * sobre un directorio temporal real.
 */
class BackupFileStorageTest {

    private BackupFileStorage storage(Path base) {
        BackupProperties propiedades = new BackupProperties();
        propiedades.setRutaBase(base.toString());
        return new BackupFileStorage(propiedades);
    }

    @Test
    void resolverRutaDestino_CreaElDirectorioBaseSiNoExiste(@TempDir Path tempDir) throws IOException {
        Path base = tempDir.resolve("respaldos");
        BackupFileStorage storage = storage(base);

        Path resultado = storage.resolveDestinationPath("archivo.dump");

        assertThat(Files.isDirectory(base)).isTrue();
        assertThat(resultado).isEqualTo(base.resolve("archivo.dump"));
    }

    @Test
    void resolverRutaDestino_ConDirectorioBaseYaExistente_NoFalla(@TempDir Path tempDir) throws IOException {
        BackupFileStorage storage = storage(tempDir);

        Path resultado = storage.resolveDestinationPath("otro.zip");

        assertThat(resultado).isEqualTo(tempDir.resolve("otro.zip"));
    }

    @Test
    void eliminar_ArchivoExistente_LoBorra(@TempDir Path tempDir) throws IOException {
        BackupFileStorage storage = storage(tempDir);
        Path archivo = tempDir.resolve("borrar.dump");
        Files.writeString(archivo, "contenido");

        storage.delete(archivo);

        assertThat(Files.exists(archivo)).isFalse();
    }

    @Test
    void eliminar_ArchivoInexistente_NoLanza(@TempDir Path tempDir) {
        BackupFileStorage storage = storage(tempDir);
        Path archivo = tempDir.resolve("no-existe.dump");

        org.assertj.core.api.Assertions.assertThatCode(() -> storage.delete(archivo))
                .doesNotThrowAnyException();
    }
}
