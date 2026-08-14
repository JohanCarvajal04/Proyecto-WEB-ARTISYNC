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

    public static String resolver(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
