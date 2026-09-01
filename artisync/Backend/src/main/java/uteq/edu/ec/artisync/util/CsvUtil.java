package uteq.edu.ec.artisync.util;

/**
 * Extraído de AuditServiceImpl (RNF-13) para que AuditoriaServicioImpl no
 * duplique el mismo escapado.
 */
public final class CsvUtil {

    /** BOM UTF-8: sin él, Excel en español interpreta el archivo como ANSI y
     *  rompe las tildes. AuditServiceImpl no lo lleva (bug preexistente, fuera
     *  de alcance de este módulo); el CSV de auditoría sí lo antepone. */
    public static final String BOM_UTF8 = "﻿";

    private CsvUtil() {
    }

    /**
     * Caracteres que Excel/LibreOffice interpretan como inicio de fórmula al
     * abrir un CSV. Sin neutralizarlos, un dato de usuario que termina en un
     * CSV exportado (título de servicio, nombre de usuario, ambos editables
     * por su dueño) puede ejecutar una fórmula -- incluida una que llame a
     * un programa externo -- en la máquina de quien abre el reporte (p. ej.
     * un admin auditando exportaciones). Mitigación estándar de OWASP (CSV
     * Injection): anteponer un apóstrofo fuerza a la hoja de cálculo a
     * tratar la celda como texto en vez de evaluarla.
     */
    private static final String CARACTERES_FORMULA = "=+-@\t\r";

    /** Escapa caracteres especiales de CSV (comillas, comas, saltos de línea) y neutraliza inyección de fórmulas. */
    public static String escapeCsv(String valor) {
        if (valor == null) {
            return "";
        }
        if (!valor.isEmpty() && CARACTERES_FORMULA.indexOf(valor.charAt(0)) >= 0) {
            valor = "'" + valor;
        }
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
