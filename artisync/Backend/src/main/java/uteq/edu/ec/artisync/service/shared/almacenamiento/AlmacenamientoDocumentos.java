package uteq.edu.ec.artisync.service.shared.almacenamiento;

import org.springframework.web.multipart.MultipartFile;

/**
 * Persistencia de documentos de verificación. AlmacenamientoLocal es la
 * implementación actual (volumen Docker); esta interfaz deja la puerta
 * abierta a MinIO/S3 sin tocar a los consumidores (VerificacionServicioImpl).
 */
public interface AlmacenamientoDocumentos {
    String guardar(MultipartFile archivo);
    byte[] leer(String referencia);
    void eliminar(String referencia);
}
