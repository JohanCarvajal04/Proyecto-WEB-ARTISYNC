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

    /**
     * Solo true para fallos transitorios (429, timeout): un segundo intento
     * puede tener éxito. Para 401/413 el segundo intento fallaría igual y
     * solo duplicaría la espera del moderador.
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
