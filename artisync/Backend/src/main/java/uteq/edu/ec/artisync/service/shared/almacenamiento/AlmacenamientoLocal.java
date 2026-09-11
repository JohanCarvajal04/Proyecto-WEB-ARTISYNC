package uteq.edu.ec.artisync.service.shared.almacenamiento;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.config.StorageProperties;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;

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

    public AlmacenamientoLocal(StorageProperties propiedades) {
        this.rutaBase = Paths.get(propiedades.getRutaBase()).toAbsolutePath().normalize();
        log.info("Almacenamiento local de documentos configurado en {}", rutaBase);
    }

    private void asegurarDirectorio() {
        try {
            Files.createDirectories(rutaBase);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo crear el directorio de documentos: " + rutaBase, e);
        }
    }

    @Override
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param archivo objeto binario multipart representando el documento o medio fisico
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public String guardar(MultipartFile archivo) {
        return guardar(archivo, "");
    }

    @Override
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param archivo objeto binario multipart representando el documento o medio fisico
     * @param prefijo parametro requerido para la correcta ejecucion del procedimiento
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
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
            throw new BusinessRuleException("No se pudo guardar el documento: " + e.getMessage());
        }
        return nombre;
    }

    /** El almacenamiento local no sabe firmar URLs: el consumidor sirve los bytes. */
    @Override
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param referencia parametro requerido para la correcta ejecucion del procedimiento
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Optional<String> urlTemporal(String referencia) {
        return Optional.empty();
    }

    @Override
    public byte[] leer(String referencia) {
        Path ruta = resolverDentroDeBase(referencia);
        try {
            return Files.readAllBytes(ruta);
        } catch (IOException e) {
            throw new ResourceNotFoundException("Documento no disponible: " + referencia);
        }
    }

    @Override
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param referencia parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void eliminar(String referencia) {
        Path ruta = resolverDentroDeBase(referencia);
        try {
            Files.deleteIfExists(ruta);
        } catch (IOException e) {
            throw new BusinessRuleException("No se pudo eliminar el documento: " + e.getMessage());
        }
    }

    private Path resolverDentroDeBase(String referencia) {
        Path ruta = rutaBase.resolve(referencia).normalize();
        if (!ruta.startsWith(rutaBase)) {
            throw new BusinessRuleException("Referencia de documento inválida.");
        }
        return ruta;
    }
}
