package uteq.edu.ec.artisync.service.shared.ia;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Slf4j
public abstract class AbstractIaService {

    protected String cargarPrompt(String nombreArchivo, Object... args) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("IA/" + nombreArchivo)) {
            if (is == null) {
                throw new IllegalStateException("Prompt no encontrado: " + nombreArchivo);
            }
            String plantilla = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            return args.length > 0 ? String.format(plantilla, args) : plantilla;
        } catch (IOException e) {
            throw new IllegalStateException("Error al cargar prompt: " + nombreArchivo, e);
        }
    }

    protected String extraerJson(String respuesta) {
        if (respuesta == null || respuesta.isBlank()) {
            return "{}";
        }
        String recortado = respuesta.trim();
        if (recortado.contains("```json")) {
            int inicio = recortado.indexOf("```json") + 7;
            int fin = recortado.indexOf("```", inicio);
            if (fin > inicio) {
                return recortado.substring(inicio, fin).trim();
            }
        }
        if (recortado.startsWith("```") && recortado.endsWith("```")) {
            return recortado.substring(3, recortado.length() - 3).trim();
        }
        if (recortado.startsWith("{") && recortado.endsWith("}")) {
            return recortado;
        }
        int inicio = recortado.indexOf('{');
        int fin = recortado.lastIndexOf('}');
        if (inicio >= 0 && fin > inicio) {
            return recortado.substring(inicio, fin + 1);
        }
        log.warn("No se pudo extraer JSON válido de la respuesta de IA: {}",
                recortado.substring(0, Math.min(100, recortado.length())));
        return "{}";
    }

    /**
     * Lee un campo de texto distinguiendo "vale null" y "no vino" de un valor
     * real — ambos deben mapear a null de Java, nunca a la cadena "null".
     */
    protected String textoONull(JsonNode nodo, String campo) {
        JsonNode valor = nodo.path(campo);
        if (valor.isNull() || valor.isMissingNode()) {
            return null;
        }
        return valor.asString();
    }

    protected BigDecimal toBigDecimal(Object valor) {
        if (valor == null) return BigDecimal.ZERO;
        if (valor instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        try {
            return new BigDecimal(valor.toString());
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    /** Acota la confianza reportada por la IA a [0,1] (límite de la columna en BD). */
    protected BigDecimal acotarConfianza(BigDecimal confianza) {
        if (confianza == null) return BigDecimal.ZERO;
        if (confianza.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
        if (confianza.compareTo(BigDecimal.ONE) > 0) return BigDecimal.ONE;
        return confianza;
    }
}
