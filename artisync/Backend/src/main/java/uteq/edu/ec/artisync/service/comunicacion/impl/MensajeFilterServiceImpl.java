package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.springframework.stereotype.Service;
import uteq.edu.ec.artisync.service.comunicacion.MensajeFilterService;

import java.util.regex.Pattern;

/**
 * Implementación del filtro de datos de contacto.
 * RF-15: Detecta teléfonos (formatos internacionales y locales) y correos electrónicos.
 */
@Service
public class MensajeFilterServiceImpl implements MensajeFilterService {

    /**
     * Patrones de detección de números telefónicos:
     * - +593 99 123 4567 (formato internacional con prefijo +)
     * - (02) 123456       (código de área entre paréntesis)
     * - 123-456-7890      (formato norteamericano con guiones)
     * - 10+ dígitos consecutivos
     */
    private static final Pattern PATRON_TELEFONO = Pattern.compile(
            "(\\+?\\d[\\d\\-\\s]{7,})"          // +dígitos con separadores (≥8 caracteres)
            + "|"
            + "(\\(\\d{2,4}\\)\\s?\\d{3,})"     // (02) 123456
            + "|"
            + "(\\d{3}[\\-.]\\d{3}[\\-.]\\d{4})" // 123-456-7890
    );

    /**
     * Patrón de detección de correos electrónicos.
     */
    private static final Pattern PATRON_EMAIL = Pattern.compile(
            "[\\w.+-]+@[\\w-]+\\.[\\w.]+"
    );

    @Override
    public boolean contieneContacto(String texto) {
        if (texto == null || texto.isBlank()) {
            return false;
        }
        return PATRON_TELEFONO.matcher(texto).find()
                || PATRON_EMAIL.matcher(texto).find();
    }

    @Override
    public String detectarPatron(String texto) {
        if (texto == null || texto.isBlank()) {
            return "DESCONOCIDO";
        }
        if (PATRON_EMAIL.matcher(texto).find()) {
            return "EMAIL";
        }
        if (PATRON_TELEFONO.matcher(texto).find()) {
            return "TELEFONO";
        }
        return "DESCONOCIDO";
    }
}
