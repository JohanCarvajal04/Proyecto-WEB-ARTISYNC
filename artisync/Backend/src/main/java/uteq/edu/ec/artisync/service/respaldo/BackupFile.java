package uteq.edu.ec.artisync.service.respaldo;

import org.springframework.core.io.Resource;

/**
 * Archivo de un respaldo listo para transmitirse en streaming (ver BackupController#descargar).
 *
 * @param recurso contenido binario del respaldo, listo para escribirse en la respuesta HTTP
 * @param nombreArchivo nombre con el que se ofrece la descarga al cliente
 * @param tamanoBytes tamaño del archivo en bytes, usado para la cabecera Content-Length
 */
public record BackupFile(Resource recurso, String nombreArchivo, long tamanoBytes) {
}
