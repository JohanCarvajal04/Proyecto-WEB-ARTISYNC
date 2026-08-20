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

    /** Escapa caracteres especiales de CSV (comillas, comas, saltos de línea). */
    public static String escapeCsv(String valor) {
        if (valor == null) {
            return "";
        }
        if (valor.contains(",") || valor.contains("\"") || valor.contains("\n")) {
            return "\"" + valor.replace("\"", "\"\"") + "\"";
        }
        return valor;
    }
}
