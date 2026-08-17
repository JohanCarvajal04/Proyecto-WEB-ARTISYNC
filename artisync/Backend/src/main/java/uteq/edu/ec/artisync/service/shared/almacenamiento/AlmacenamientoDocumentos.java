package uteq.edu.ec.artisync.service.shared.almacenamiento;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Persistencia de archivos de la plataforma. AlmacenamientoLocal (volumen del
 * contenedor) y AlmacenamientoAzure (Blob Storage) la implementan; cuál se
 * registra lo decide "documentos.proveedor" — ver ADR-007.
 */
public interface AlmacenamientoDocumentos {

    /**
     * @deprecated Sin prefijo no hay forma de decidir a qué backend va el
     *     archivo. AlmacenamientoRouter lo manda al volumen local. Se conserva
     *     solo por las referencias que se escribieron así antes del enrutado;
     *     use {@link #guardar(MultipartFile, String)}.
     */
    @Deprecated
    String guardar(MultipartFile archivo);

    /**
     * Guarda bajo un prefijo lógico ("verificacion", "portafolio",
     * "entregables"), que separa por caso de uso lo que de otro modo sería un
     * único espacio plano. La referencia devuelta ya lo incluye, así que leer()
     * y eliminar() la reciben tal cual salió de aquí.
     */
    String guardar(MultipartFile archivo, String prefijo);

    byte[] leer(String referencia);

    void eliminar(String referencia);

    /**
     * URL firmada y de vigencia corta para que el cliente descargue el archivo
     * sin pasar por el backend. Vacío si el proveedor no sabe emitirlas —
     * almacenamiento local no tiene manera—, en cuyo caso el consumidor debe
     * caer a servir los bytes con leer().
     *
     * <p>Importa para video de portafolio: proxear cientos de MB por el backend
     * cuando Azure puede servirlos directamente es desperdiciar el servidor.
     */
    Optional<String> urlTemporal(String referencia);
}
