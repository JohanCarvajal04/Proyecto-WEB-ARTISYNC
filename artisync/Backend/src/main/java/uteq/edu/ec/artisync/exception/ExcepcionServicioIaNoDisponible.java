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
public class ExcepcionServicioIaNoDisponible extends RuntimeException {

    /**
     * Solo true para fallos transitorios (429, timeout): un segundo intento
     * puede tener Ã©xito. Para 401/413 el segundo intento fallarÃ­a igual y
     * solo duplicarÃ­a la espera del moderador.
     */
    private final boolean reintentable;

    public ExcepcionServicioIaNoDisponible(String mensaje, Throwable causa) {
        this(mensaje, causa, false);
    }

    public ExcepcionServicioIaNoDisponible(String mensaje, Throwable causa, boolean reintentable) {
        super(mensaje, causa);
        this.reintentable = reintentable;
    }

    public boolean isReintentable() {
        return reintentable;
    }
}

