package uteq.edu.ec.artisync.service.respaldo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.config.BackupProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Almacenamiento local de archivos de respaldo. A propósito sin abstracción
 * de proveedor (a diferencia de DocumentStorage, que sí tiene
 * local/Azure): el alcance pedido es solo local por ahora. Este componente es
 * el punto de extensión natural si más adelante se quiere almacenamiento en
 * la nube, pero esa interfaz no se construye ahora sin necesidad real.
 */
@Component
@RequiredArgsConstructor
public class BackupFileStorage {

    private final BackupProperties respaldoProperties;

    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param nombreArchivo objeto binario multipart representando el documento o medio fisico
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Path resolveDestinationPath(String nombreArchivo) throws IOException {
        Path base = Path.of(respaldoProperties.getRutaBase());
        Files.createDirectories(base);
        return base.resolve(nombreArchivo);
    }

    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param ruta parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void delete(Path ruta) throws IOException {
        Files.deleteIfExists(ruta);
    }
}
