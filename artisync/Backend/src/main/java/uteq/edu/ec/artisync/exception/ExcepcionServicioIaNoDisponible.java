package uteq.edu.ec.artisync.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * La IA no pudo emitir un dictamen (fallo de red, 401/413/429/5xx de NVIDIA,
 * o respuesta ilegible). No representa un rechazo del documento: la fila de
 * verificación queda intacta y el moderador revisa sin asistencia.
 */
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class ExcepcionServicioIaNoDisponible extends RuntimeException {
    public ExcepcionServicioIaNoDisponible(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
