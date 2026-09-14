package uteq.edu.ec.artisync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Componente de Excepciones: Error de integracion.
 * 
 * Propósito: Alertar de fallos o indisponibilidad temporal en los servicios externos de Inteligencia Artificial.
 * 
 * Flujo interno: Traducida a HTTP 502 Bad Gateway o 503 Service Unavailable, alertando al cliente de un problema de terceros.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiServiceUnavailableException extends RuntimeException {

    /**
     * Solo true para fallos transitorios (429, timeout): un segundo intento
     * puede tener éxito. Para 401/413 el segundo intento fallaría igual y
     * solo duplicaría la espera del moderador.
     */
    private final boolean reintentable;

    /**
     * Crea la excepción marcándola como no reintentable por defecto (p. ej.
     * 401/413, donde un segundo intento fallaría igual).
     *
     * @param mensaje descripción del fallo del proveedor de IA
     * @param causa excepción original que originó el fallo
     */
    public AiServiceUnavailableException(String mensaje, Throwable causa) {
        this(mensaje, causa, false);
    }

    /**
     * Crea la excepción indicando explícitamente si el fallo es transitorio.
     *
     * @param mensaje descripción del fallo del proveedor de IA
     * @param causa excepción original que originó el fallo
     * @param reintentable {@code true} si el fallo es transitorio (429, timeout) y
     *                      un reintento razonable podría tener éxito
     */
    public AiServiceUnavailableException(String mensaje, Throwable causa, boolean reintentable) {
        super(mensaje, causa);
        this.reintentable = reintentable;
    }

    /**
     * @return {@code true} si el fallo es transitorio (429, timeout) y un
     *         reintento razonable podría tener éxito; {@code false} para
     *         fallos que un reintento no resolvería (401, 413)
     */
    public boolean isReintentable() {
        return reintentable;
    }
}

