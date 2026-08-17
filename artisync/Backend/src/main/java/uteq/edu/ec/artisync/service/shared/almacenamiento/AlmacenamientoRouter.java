package uteq.edu.ec.artisync.service.shared.almacenamiento;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.config.AlmacenamientoProperties;

import java.util.Optional;
import java.util.Set;

/**
 * Reparte los archivos entre los dos backends, que conviven en lugar de
 * excluirse. Es la implementación que reciben los servicios al inyectar
 * AlmacenamientoDocumentos — ver ADR-007.
 *
 * <p>El criterio es el prefijo: los de {@code documentos.prefijos-locales}
 * (por defecto solo "verificacion") van al volumen del contenedor porque
 * caducan a los 30 días y son los documentos más sensibles; el resto va a
 * Azure. Al leer o borrar el prefijo se extrae de la propia referencia, de modo
 * que cada archivo vuelve al backend donde se escribió.
 */
@Slf4j
@Primary
@Component
public class AlmacenamientoRouter implements AlmacenamientoDocumentos {

    private final AlmacenamientoLocal local;
    private final ObjectProvider<AlmacenamientoAzure> azureProvider;
    private final Set<String> prefijosLocales;

    public AlmacenamientoRouter(AlmacenamientoLocal local,
                                ObjectProvider<AlmacenamientoAzure> azureProvider,
                                AlmacenamientoProperties propiedades) {
        this.local = local;
        this.azureProvider = azureProvider;
        this.prefijosLocales = Set.copyOf(propiedades.getPrefijosLocales());
    }

    @PostConstruct
    void reportarConfiguracion() {
        if (azureDisponible()) {
            log.info("Almacenamiento dual: prefijos {} en volumen local, el resto en Azure",
                    prefijosLocales);
        } else {
            log.warn("Azure no está configurado (documentos.azure.connection-string vacío): "
                    + "TODOS los archivos se guardan en el volumen local. "
                    + "En producción esto es un error de despliegue.");
        }
    }

    private boolean azureDisponible() {
        return azureProvider.getIfAvailable() != null;
    }

    /**
     * Backend que corresponde a un prefijo. Sin Azure configurado todo cae al
     * volumen local: es preferible a rechazar la subida, y el aviso de arranque
     * deja constancia.
     */
    private AlmacenamientoDocumentos backendPara(String prefijo) {
        if (prefijo != null && prefijosLocales.contains(prefijo)) {
            return local;
        }
        AlmacenamientoDocumentos azure = azureProvider.getIfAvailable();
        return azure != null ? azure : local;
    }

    /**
     * @deprecated No permite decidir backend, así que enruta al volumen local.
     *     Se conserva porque las referencias de verificación anteriores al
     *     enrutado se guardaron con este método y viven ahí. Todo código nuevo
     *     debe usar {@link #guardar(MultipartFile, String)} con su prefijo.
     */
    @Override
    @Deprecated
    public String guardar(MultipartFile archivo) {
        log.warn("guardar() sin prefijo: el archivo va al volumen local por defecto.");
        return local.guardar(archivo);
    }

    @Override
    public String guardar(MultipartFile archivo, String prefijo) {
        return backendPara(prefijo).guardar(archivo, prefijo);
    }

    @Override
    public byte[] leer(String referencia) {
        return backendDe(referencia).leer(referencia);
    }

    @Override
    public void eliminar(String referencia) {
        backendDe(referencia).eliminar(referencia);
    }

    @Override
    public Optional<String> urlTemporal(String referencia) {
        return backendDe(referencia).urlTemporal(referencia);
    }

    /**
     * Backend donde vive una referencia ya guardada.
     *
     * <p>Una referencia sin prefijo se resuelve como local, no como Azure: las
     * de verificación anteriores al enrutado tienen esa forma y están en el
     * volumen. Mandarlas a Azure devolvería 404 a cada cédula ya subida y
     * dejaría los archivos locales sin purgar al caducar.
     */
    private AlmacenamientoDocumentos backendDe(String referencia) {
        String prefijo = PrefijoAlmacenamiento.extraer(referencia);
        if (prefijo.isEmpty()) {
            return local;
        }
        return backendPara(prefijo);
    }
}
