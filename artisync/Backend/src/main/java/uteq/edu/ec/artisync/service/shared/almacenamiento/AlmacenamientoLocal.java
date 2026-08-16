package uteq.edu.ec.artisync.service.shared.almacenamiento;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.config.AlmacenamientoProperties;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "documentos.proveedor", havingValue = "local", matchIfMissing = true)
public class AlmacenamientoLocal implements AlmacenamientoDocumentos {

    private final Path rutaBase;

    public AlmacenamientoLocal(AlmacenamientoProperties propiedades) {
        this.rutaBase = Paths.get(propiedades.getRutaBase()).toAbsolutePath().normalize();
        log.info("Almacenamiento local de documentos configurado en {}", rutaBase);
    }

    private void asegurarDirectorio() {
        try {
            Files.createDirectories(rutaBase);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear el directorio de documentos: " + rutaBase, e);
        }
    }

    @Override
    public String guardar(MultipartFile archivo) {
        return guardar(archivo, "");
    }

    @Override
    public String guardar(MultipartFile archivo, String prefijo) {
        asegurarDirectorio();
        String nombre = PrefijoAlmacenamiento.componer(
                prefijo, UUID.randomUUID() + ExtensionesArchivo.desde(archivo.getContentType()));
        Path destino = resolverDentroDeBase(nombre);
        try (InputStream in = archivo.getInputStream()) {
            // El prefijo se traduce a subdirectorio; sin esto Files.copy falla.
            Files.createDirectories(destino.getParent());
            Files.copy(in, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ExcepcionReglaNegocio("No se pudo guardar el documento: " + e.getMessage());
        }
        return nombre;
    }

    /** El almacenamiento local no sabe firmar URLs: el consumidor sirve los bytes. */
    @Override
    public Optional<String> urlTemporal(String referencia) {
        return Optional.empty();
    }

    @Override
    public byte[] leer(String referencia) {
        Path ruta = resolverDentroDeBase(referencia);
        try {
            return Files.readAllBytes(ruta);
        } catch (IOException e) {
            throw new ExcepcionRecursoNoEncontrado("Documento no disponible: " + referencia);
        }
    }

    @Override
    public void eliminar(String referencia) {
        Path ruta = resolverDentroDeBase(referencia);
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            throw new ExcepcionReglaNegocio("No se pudo eliminar el documento: " + e.getMessage());
        }
    }

    private Path resolverDentroDeBase(String referencia) {
        Path ruta = rutaBase.resolve(referencia).normalize();
        if (!ruta.startsWith(rutaBase)) {
            throw new ExcepcionReglaNegocio("Referencia de documento inválida.");
        }
        return ruta;
    }
}
