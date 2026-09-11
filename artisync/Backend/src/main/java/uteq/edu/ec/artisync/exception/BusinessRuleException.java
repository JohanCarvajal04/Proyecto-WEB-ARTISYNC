package uteq.edu.ec.artisync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Componente de Excepciones: Error general de dominio.
 * 
 * Propósito: Lanzar errores controlados cuando se incumple una politica de negocio del modelo (ej. estado invalido para una transicion).
 * 
 * Flujo interno: Traducida por el manejador global a HTTP 400 Bad Request o 422 Unprocessable Entity.
 */
@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String mensaje) {
        super(mensaje);
    }
}

