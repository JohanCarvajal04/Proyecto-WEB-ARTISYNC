package uteq.edu.ec.artisync.service.shared.almacenamiento;

import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;

/**
 * Prefijos con los que se agrupan los archivos por caso de uso. Son parte de la
 * referencia que se guarda en base de datos, así que renombrar una constante
 * deja huérfanos los archivos ya subidos: solo se añaden.
 */
public final class PrefijoAlmacenamiento {

    public static final String VERIFICACION = "verificacion";
    public static final String PORTAFOLIO = "portafolio";
    public static final String ENTREGABLES = "entregables";

    private PrefijoAlmacenamiento() {
    }

    /**
     * Une prefijo y nombre en una referencia. El prefijo lo fija el código, no
     * el cliente, pero se valida igual: es lo que impide que una referencia
     * termine apuntando fuera de su carpeta, tanto en disco como en el blob.
     */
    static String componer(String prefijo, String nombre) {
        if (prefijo == null || prefijo.isBlank()) {
            return nombre;
        }
        String limpio = prefijo.trim();
        if (!limpio.matches("[a-z0-9-]+")) {
            throw new ExcepcionReglaNegocio("Prefijo de almacenamiento inválido: " + prefijo);
        }
        return limpio + "/" + nombre;
    }
}
