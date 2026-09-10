package uteq.edu.ec.artisync.service.respaldo;

import org.springframework.core.io.Resource;

/** Archivo de un respaldo listo para transmitirse en streaming (ver RespaldoControlador#descargar). */
public record ArchivoRespaldo(Resource recurso, String nombreArchivo, long tamanoBytes) {
}
