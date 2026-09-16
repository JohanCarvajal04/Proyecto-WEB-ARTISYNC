package uteq.edu.ec.artisync.service.shared.storage;

import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

/**
 * Persistencia de archivos de la plataforma. LocalStorage (volumen del
 * contenedor) y AzureStorage (Blob Storage) la implementan; cuál se
 * registra lo decide "documentos.proveedor" — ver ADR-007.
 */
public interface DocumentStorage {

    /**
     * Guarda en la raíz. Equivale a save(archivo, "").
     * @param archivo el archivo
     * @return el resultado en forma de texto
     */
    String save(MultipartFile archivo);

    /**
     * Guarda bajo un prefijo lógico ("verificacion", "portafolio",
     * "entregables"), que separa por caso de uso lo que de otro modo sería un
     * único espacio plano. La referencia devuelta ya lo incluye, así que read()
     * y delete() la reciben tal cual salió de aquí.
     * @param archivo el archivo
     * @param prefijo el prefijo
     * @return el resultado en forma de texto
     */
    String save(MultipartFile archivo, String prefijo);

    /**
     * Lee el contenido binario de un archivo guardado.
     * @param referencia referencia devuelta por {@link #save(MultipartFile, String)}
     * @return el contenido del archivo
     */
    byte[] read(String referencia);

    /**
     * Elimina un archivo guardado. No falla si ya no existe.
     * @param referencia referencia devuelta por {@link #save(MultipartFile, String)}
     */
    void delete(String referencia);

    /**
     * URL firmada y de vigencia corta para que el cliente descargue el archivo
     * sin pasar por el backend. Vacío si el proveedor no sabe emitirlas —
     * almacenamiento local no tiene manera—, en cuyo caso el consumidor debe
     * caer a servir los bytes con read().
     *
     * <p>Importa para video de portafolio: proxear cientos de MB por el backend
     * cuando Azure puede servirlos directamente es desperdiciar el servidor.
     * @param referencia el referencia
     * @return un Optional con String si existe, vacio en caso contrario
     */
    Optional<String> urlTemporal(String referencia);
}
