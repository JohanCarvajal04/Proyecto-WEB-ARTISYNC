package uteq.edu.ec.artisync.service.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uteq.edu.ec.artisync.security.ClientIpResolver;

/**
 * Metadatos HTTP de la petición en curso, para uso de infraestructura
 * transversal (AuditAspect). Mismo patrón que
 * AuthServiceImpl.obtenerIpActual(): todo null cuando no hay una petición
 * HTTP en el hilo actual (tareas @Scheduled, colas, hilos de @Async).
 */
public final class RequestContext {

    private RequestContext() {
    }

    /**
     * Metadatos de la petición HTTP actual, capturados para auditoría.
     *
     * @param direccionIp dirección IP de origen de la petición
     * @param agenteUsuario cabecera User-Agent del cliente
     * @param metodoHttp método HTTP de la petición (GET, POST, ...)
     * @param rutaSolicitud ruta solicitada
     */
    public record Data(String direccionIp, String agenteUsuario, String metodoHttp, String rutaSolicitud) {
    }

    private static final Data VACIO = new Data(null, null, null, null);

    /**
     * Captura los metadatos HTTP de la petición en curso.
     * @return los metadatos de la petición actual, o {@link #VACIO} si no hay una petición HTTP en el hilo actual
     */
    public static Data current() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return VACIO;
        }
        HttpServletRequest request = attributes.getRequest();
        return new Data(
                ClientIpResolver.resolve(request),
                request.getHeader("User-Agent"),
                request.getMethod(),
                request.getRequestURI()
        );
    }
}
