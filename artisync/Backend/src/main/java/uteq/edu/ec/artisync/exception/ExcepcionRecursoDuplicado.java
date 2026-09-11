package uteq.edu.ec.artisync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Componente de Excepciones: Error personalizado de negocio.
 * 
 * Propósito: Notificar intentos de crear un recurso (correo, nombre) que vulnera una restriccion de unicidad.
 * 
 * Flujo interno: Traducida por el manejador global a un codigo HTTP 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ExcepcionRecursoDuplicado extends RuntimeException {
    public ExcepcionRecursoDuplicado(String mensaje) {
        super(mensaje);
    }
}

