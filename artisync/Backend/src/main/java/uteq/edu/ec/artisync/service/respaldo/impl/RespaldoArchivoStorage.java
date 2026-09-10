package uteq.edu.ec.artisync.service.respaldo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.config.RespaldoProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Almacenamiento local de archivos de respaldo. A propósito sin abstracción
 * de proveedor (a diferencia de AlmacenamientoDocumentos, que sí tiene
 * local/Azure): el alcance pedido es solo local por ahora. Este componente es
 * el punto de extensión natural si más adelante se quiere almacenamiento en
 * la nube, pero esa interfaz no se construye ahora sin necesidad real.
 */
@Component
@RequiredArgsConstructor
public class RespaldoArchivoStorage {

    private final RespaldoProperties respaldoProperties;

    public Path resolverRutaDestino(String nombreArchivo) throws IOException {
        Path base = Path.of(respaldoProperties.getRutaBase());
        Files.createDirectories(base);
        return base.resolve(nombreArchivo);
    }

    public void eliminar(Path ruta) throws IOException {
        Files.deleteIfExists(ruta);
    }
}
