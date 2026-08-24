package uteq.edu.ec.artisync.service.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uteq.edu.ec.artisync.security.ClientIpResolver;

/**
 * Metadatos HTTP de la petición en curso, para uso de infraestructura
 * transversal (AspectoAuditoria). Mismo patrón que
 * AuthServiceImpl.obtenerIpActual(): todo null cuando no hay una petición
 * HTTP en el hilo actual (tareas @Scheduled, colas, hilos de @Async).
 */
public final class ContextoSolicitud {

    private ContextoSolicitud() {
    }

    public record Datos(String direccionIp, String agenteUsuario, String metodoHttp, String rutaSolicitud) {
    }

    private static final Datos VACIO = new Datos(null, null, null, null);

    public static Datos actual() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return VACIO;
        }
        HttpServletRequest request = attributes.getRequest();
        return new Datos(
                ClientIpResolver.resolver(request),
                request.getHeader("User-Agent"),
                request.getMethod(),
                request.getRequestURI()
        );
    }
}
