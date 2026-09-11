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
public final class ContextoSolicitud {

    private ContextoSolicitud() {
    }

    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param direccionIp parametro requerido para la correcta ejecucion del procedimiento
     * @param agenteUsuario parametro requerido para la correcta ejecucion del procedimiento
     * @param metodoHttp parametro requerido para la correcta ejecucion del procedimiento
     * @param rutaSolicitud parametro requerido para la correcta ejecucion del procedimiento
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
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
