package uteq.edu.ec.artisync.service.shared.almacenamiento;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.config.AlmacenamientoProperties;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de archivos en Azure Blob Storage. Convive con
 * AlmacenamientoLocal: AlmacenamientoRouter decide por prefijo cuál atiende
 * cada archivo — ver ADR-007.
 *
 * <p>El bean solo se registra si hay cadena de conexión. Así el backend arranca
 * en CI y en desarrollo sin credenciales de Azure ni emulador; el router lo nota
 * y enruta todo al volumen local.
 *
 * <p>La referencia que devuelve guardar() es el nombre del blob, no su URL: el
 * contenedor es privado, así que una URL directa no sirve para nada. Para
 * exponer un archivo al frontend sin proxearlo por el backend está
 * urlTemporal(), que firma un SAS de vigencia corta.
 */
@Slf4j
@Component
@ConditionalOnExpression("!'${documentos.azure.connection-string:}'.trim().isEmpty()")
public class AlmacenamientoAzure implements AlmacenamientoDocumentos {

    private final BlobContainerClient contenedor;
    private final long sasMinutos;
    private volatile boolean contenedorVerificado = false;

    public AlmacenamientoAzure(AlmacenamientoProperties propiedades) {
        AlmacenamientoProperties.Azure config = propiedades.getAzure();
        if (config.getConnectionString() == null || config.getConnectionString().isBlank()) {
            // Defensivo: la condición de registro del bean ya exige la cadena.
            throw new IllegalStateException(
                    "AlmacenamientoAzure requiere documentos.azure.connection-string "
                            + "(variable de entorno AZURE_STORAGE_CONNECTION_STRING).");
        }
        this.contenedor = new BlobServiceClientBuilder()
                .connectionString(config.getConnectionString())
                .buildClient()
                .getBlobContainerClient(config.getContenedor());
        this.sasMinutos = config.getSasMinutos();
        log.info("Almacenamiento de documentos en Azure, contenedor '{}'", config.getContenedor());
    }

    /**
     * Crea el contenedor la primera vez que se usa. No se hace en el constructor
     * a propósito: arrancar la aplicación no debe depender de que la red y las
     * credenciales de Azure respondan.
     */
    private void asegurarContenedor() {
        if (contenedorVerificado) {
            return;
        }
        synchronized (this) {
            if (contenedorVerificado) {
                return;
            }
            try {
                if (!contenedor.exists()) {
                    contenedor.create();
                    log.info("Contenedor '{}' creado en Azure", contenedor.getBlobContainerName());
                }
                contenedorVerificado = true;
            } catch (BlobStorageException e) {
                throw new ExcepcionReglaNegocio(
                        "No se pudo acceder al contenedor de Azure: " + e.getServiceMessage());
            }
        }
    }

    @Override
    public String guardar(MultipartFile archivo) {
        return guardar(archivo, "");
    }

    @Override
    public String guardar(MultipartFile archivo, String prefijo) {
        asegurarContenedor();
        String nombreBlob = PrefijoAlmacenamiento.componer(
                prefijo, UUID.randomUUID() + ExtensionesArchivo.desde(archivo.getContentType()));
        BlobClient blob = contenedor.getBlobClient(nombreBlob);

        try (InputStream entrada = archivo.getInputStream()) {
            blob.upload(entrada, archivo.getSize(), true);
            blob.setHttpHeaders(new BlobHttpHeaders().setContentType(archivo.getContentType()));
        } catch (IOException | BlobStorageException e) {
            throw new ExcepcionReglaNegocio("No se pudo guardar el documento: " + e.getMessage());
        }

        log.info("Documento guardado en Azure: {}", nombreBlob);
        return nombreBlob;
    }

    @Override
    public byte[] leer(String referencia) {
        validarReferencia(referencia);
        asegurarContenedor();
        BlobClient blob = contenedor.getBlobClient(referencia);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            blob.downloadStream(salida);
        } catch (BlobStorageException e) {
            if (e.getStatusCode() == 404) {
                throw new ExcepcionRecursoNoEncontrado("Documento no disponible: " + referencia);
            }
            throw new ExcepcionReglaNegocio("No se pudo leer el documento: " + e.getServiceMessage());
        }
        return salida.toByteArray();
    }

    @Override
    public void eliminar(String referencia) {
        validarReferencia(referencia);
        asegurarContenedor();
        BlobClient blob = contenedor.getBlobClient(referencia);
        try {
            blob.deleteIfExists();
        } catch (BlobStorageException e) {
            throw new ExcepcionReglaNegocio("No se pudo eliminar el documento: " + e.getServiceMessage());
        }
    }

    /**
     * URL firmada de solo lectura para que el frontend descargue el archivo
     * directamente de Azure. Pensada para portafolio y entregables, donde
     * proxear video por el backend sería un desperdicio.
     */
    @Override
    public Optional<String> urlTemporal(String referencia) {
        validarReferencia(referencia);
        asegurarContenedor();
        BlobClient blob = contenedor.getBlobClient(referencia);
        BlobServiceSasSignatureValues valores = new BlobServiceSasSignatureValues(
                OffsetDateTime.now().plusMinutes(sasMinutos),
                new BlobSasPermission().setReadPermission(true));
        return Optional.of(blob.getBlobUrl() + "?" + blob.generateSas(valores));
    }

    /**
     * Un blob no es una ruta de disco, pero la referencia llega desde la base de
     * datos y no tiene por qué escaparse del prefijo que le corresponde.
     */
    private void validarReferencia(String referencia) {
        if (referencia == null || referencia.isBlank()
                || referencia.startsWith("/")
                || referencia.contains("..")) {
            throw new ExcepcionReglaNegocio("Referencia de documento inválida.");
        }
    }
}
