package uteq.edu.ec.artisync.service.shared.reporte.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorGraficaReporteTest {

    private final GeneradorGraficaReporte generador = new GeneradorGraficaReporte();

    @Test
    @DisplayName("Genera gráfica de usuarios por rol como PNG válido")
    void generarGraficaRol_GeneraPngValido() {
        Map<String, Long> datos = new LinkedHashMap<>();
        datos.put("CLIENTE", 15L);
        datos.put("CREADOR", 8L);
        datos.put("ADMIN", 2L);
        datos.put("MODERADOR", 1L);

        byte[] png = generador.generarGraficaRol(datos);

        assertThat(png).isNotEmpty();
        // Firma de archivo PNG: 89 50 4E 47 0D 0A 1A 0A
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 0x50);
        assertThat(png[2]).isEqualTo((byte) 0x4E);
        assertThat(png[3]).isEqualTo((byte) 0x47);
    }

    @Test
    @DisplayName("Genera gráfica de usuarios por país como PNG válido")
    void generarGraficaPais_GeneraPngValido() {
        Map<String, Long> datos = new LinkedHashMap<>();
        datos.put("Chile", 10L);
        datos.put("Colombia", 7L);
        datos.put("Argentina", 5L);
        datos.put("México", 3L);
        datos.put("Perú", 2L);

        byte[] png = generador.generarGraficaPais(datos);

        assertThat(png).isNotEmpty();
        assertThat(png[0]).isEqualTo((byte) 0x89);
        assertThat(png[1]).isEqualTo((byte) 0x50);
        assertThat(png[2]).isEqualTo((byte) 0x4E);
        assertThat(png[3]).isEqualTo((byte) 0x47);
    }

    @Test
    @DisplayName("Maneja datos vacíos sin lanzar excepción")
    void generarGraficas_DatosVacios_RetornaPngValido() {
        byte[] pngRol = generador.generarGraficaRol(Map.of());
        byte[] pngPais = generador.generarGraficaPais(Map.of());

        assertThat(pngRol).isNotEmpty();
        assertThat(pngPais).isNotEmpty();
    }
}
