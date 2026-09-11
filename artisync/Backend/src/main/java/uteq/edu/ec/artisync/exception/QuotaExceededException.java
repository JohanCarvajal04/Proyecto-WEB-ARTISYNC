package uteq.edu.ec.artisync.exception;

/**
 * Componente de Excepciones: Error personalizado de negocio.
 * 
 * Propósito: Notificar que un usuario ha superado un limite o cuota operativa permitida en el sistema (ej. cuota de IA).
 * 
 * Flujo interno: Lanzada por la capa de negocio y atrapada por el manejador global para traducirse a un HTTP 429 Too Many Requests o 402 Payment Required.
 */
public class QuotaExceededException extends RuntimeException {

    private final long retryAfterSegundos;

    public QuotaExceededException(String mensaje, long retryAfterSegundos) {
        super(mensaje);
        this.retryAfterSegundos = retryAfterSegundos;
    }

    public long getRetryAfterSegundos() {
        return retryAfterSegundos;
    }
}

