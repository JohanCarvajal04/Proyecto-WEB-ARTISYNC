package uteq.edu.ec.artisync.audit;

import java.time.temporal.Temporal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Prepara un mapa de detalle para persistirse en la columna JSONB
 * detalle_cambio. Hace dos cosas, las dos obligatorias y no configurables:
 *
 * 1. Enmascara por nombre de clave cualquier dato sensible (denylist),
 *    porque la primera defensa (que la anotación @Auditable declare campos
 *    concretos y no objetos completos) puede fallar por descuido humano. El
 *    fallo por omisión de este sanitizador debe ser "no se auditó el campo",
 *    nunca "se guardó la contraseña".
 *
 * 2. Normaliza cada valor a String/Number/Boolean/null. Esto no es solo
 *    higiene: ArtisyncApplication declara "new ObjectMapper()" a pelo como
 *    bean, que SOBRESCRIBE el autoconfigurado de Spring Boot y por tanto NO
 *    tiene registrado JavaTimeModule. Un LocalDateTime (o cualquier tipo
 *    java.time) dentro del mapa de detalle lanzaría
 *    InvalidDefinitionException al serializar a JSONB. Convertirlo aquí a
 *    String evita tocar ese bean compartido por toda la aplicación.
 */
public final class SanitizadorAuditoria {

    private static final int MAX_LONGITUD_STRING = 500;
    private static final int MAX_LONGITUD_JSON = 8 * 1024;

    private static final List<String> CLAVES_SENSIBLES = List.of(
            "contrasena", "password", "token", "jti", "secreto", "codigo",
            "cuerpomensaje", "textomensaje", "documento", "archivo"
    );

    private SanitizadorAuditoria() {
    }

    public static Map<String, Object> sanitizar(Map<String, Object> origen) {
        if (origen == null || origen.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entrada : origen.entrySet()) {
            resultado.put(entrada.getKey(), sanitizarValor(entrada.getKey(), entrada.getValue()));
        }
        return truncarJson(resultado);
    }

    private static Object sanitizarValor(String clave, Object valor) {
        if (esClaveSensible(clave)) {
            return "***";
        }
        return normalizar(valor);
    }

    private static boolean esClaveSensible(String clave) {
        if (clave == null) {
            return false;
        }
        String normalizada = clave.toLowerCase(Locale.ROOT);
        return CLAVES_SENSIBLES.stream().anyMatch(normalizada::contains);
    }

    @SuppressWarnings("unchecked")
    private static Object normalizar(Object valor) {
        if (valor == null || valor instanceof Number || valor instanceof Boolean) {
            return valor;
        }
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> normalizado = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                String clave = String.valueOf(entrada.getKey());
                normalizado.put(clave, sanitizarValor(clave, entrada.getValue()));
            }
            return normalizado;
        }
        if (valor instanceof List<?> lista) {
            return lista.stream().map(SanitizadorAuditoria::normalizar).toList();
        }
        if (valor instanceof Temporal || valor instanceof java.util.Date) {
            return truncar(String.valueOf(valor));
        }
        return truncar(String.valueOf(valor));
    }

    private static String truncar(String texto) {
        if (texto == null || texto.length() <= MAX_LONGITUD_STRING) {
            return texto;
        }
        return texto.substring(0, MAX_LONGITUD_STRING) + "…";
    }

    /**
     * Tope defensivo sobre el tamaño total: si el mapa resultante sigue
     * siendo enorme (muchas claves, cada una dentro del límite individual),
     * se descarta el detalle y se deja constancia del motivo en su lugar.
     */
    private static Map<String, Object> truncarJson(Map<String, Object> resultado) {
        int longitudAproximada = resultado.toString().length();
        if (longitudAproximada <= MAX_LONGITUD_JSON) {
            return resultado;
        }
        Map<String, Object> recortado = new LinkedHashMap<>();
        recortado.put("_truncado", "detalle_cambio excede " + MAX_LONGITUD_JSON + " bytes; omitido");
        return recortado;
    }
}
