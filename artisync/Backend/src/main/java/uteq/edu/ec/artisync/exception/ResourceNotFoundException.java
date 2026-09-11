package uteq.edu.ec.artisync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Componente de Excepciones: Error personalizado de infraestructura/negocio.
 * 
 * Propósito: Notificar que el recurso solicitado a traves de un identificador no existe en la base de datos.
 * 
 * Flujo interno: Traducida por el manejador global a un codigo HTTP 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}

