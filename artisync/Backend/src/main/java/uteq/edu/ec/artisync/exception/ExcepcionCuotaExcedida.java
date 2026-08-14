package uteq.edu.ec.artisync.exception;

/**
 * §2.2 / OBS-AUTO-06: cuota de intentos por CUENTA excedida (a diferencia del
 * rate limit por IP de AuthRateLimitFilter, que responde 429 directamente
 * desde el filtro). Se maneja como ProblemDetail via ManejadorGlobalExcepciones
 * para que lleve la cabecera Retry-After, que un ResponseStatusException
 * generico no puede transportar.
 */
public class ExcepcionCuotaExcedida extends RuntimeException {

    private final long retryAfterSegundos;

    public ExcepcionCuotaExcedida(String mensaje, long retryAfterSegundos) {
        super(mensaje);
        this.retryAfterSegundos = retryAfterSegundos;
    }

    public long getRetryAfterSegundos() {
        return retryAfterSegundos;
    }
}
