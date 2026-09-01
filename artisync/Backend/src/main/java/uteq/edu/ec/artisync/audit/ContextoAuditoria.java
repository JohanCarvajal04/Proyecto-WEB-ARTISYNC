package uteq.edu.ec.artisync.audit;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Canal para que un service aporte datos que solo existen dentro del propio
 * método anotado con {@link Auditable} — típicamente el estado "antes" de un
 * UPDATE, que el SpEL de la anotación no puede ver porque se evalúa sobre los
 * argumentos y el resultado, no sobre el cuerpo del método.
 *
 * Uso típico:
 * <pre>
 *     ContextoAuditoria.aportar("antes", Map.of("correo", usuario.getCorreo()));
 * </pre>
 *
 * El ThreadLocal se drena y se limpia SIEMPRE en el finally de
 * {@link AspectoAuditoria#auditar}, incluso si el método lanza. Con el pool
 * de hilos de Tomcat, una fuga de este ThreadLocal contaminaría la siguiente
 * petición atendida por el mismo hilo.
 *
 * aportar() es intencionadamente un no-op seguro si no hay ningún aspecto
 * activo (el mapa se crea perezosamente): un service puede llamarlo sin
 * preocuparse de si el método que lo contiene está anotado o no.
 */
public final class ContextoAuditoria {

    private static final ThreadLocal<Map<String, Object>> DATOS = new ThreadLocal<>();

    private ContextoAuditoria() {
    }

    public static void aportar(String clave, Object valor) {
        Map<String, Object> mapa = DATOS.get();
        if (mapa == null) {
            mapa = new LinkedHashMap<>();
            DATOS.set(mapa);
        }
        mapa.put(clave, valor);
    }

    /** Solo lo llama AspectoAuditoria; limpia el ThreadLocal tras leerlo. */
    static Map<String, Object> drenar() {
        Map<String, Object> mapa = DATOS.get();
        DATOS.remove();
        return mapa == null ? Map.of() : mapa;
    }

    /** Llamado por AspectoAuditoria en el finally, incluso sin haber aportado nada. */
    static void limpiar() {
        DATOS.remove();
    }
}
