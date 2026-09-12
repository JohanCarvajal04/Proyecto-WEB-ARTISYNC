package uteq.edu.ec.artisync.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Punto unico para obtener la IP real del cliente (OBS-AUTO-06 / A07 OWASP).
 *
 * Deliberadamente NO parsea X-Forwarded-For a mano: con
 * server.forward-headers-strategy=native (application.properties), el
 * RemoteIpValve de Tomcat ya reescribe request.getRemoteAddr() usando esa
 * cabecera, validada contra server.tomcat.remoteip.internal-proxies. Esta
 * clase existe solo como costura documentada y testeable para los llamantes
 * (AuthRateLimitFilter, AuthServiceImpl.obtenerIpActual, JwtAuthenticationFilter),
 * de modo que ninguno de ellos reimplemente la logica de confianza de proxies.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    /**
     * Resuelve la IP real del cliente para la petición dada.
     *
     * @param request petición HTTP en curso
     * @return la IP del cliente, ya corregida por el {@code RemoteIpValve} de Tomcat
     *         (a partir de {@code X-Forwarded-For} cuando la petición viene de un proxy de confianza)
     */
    public static String resolver(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
