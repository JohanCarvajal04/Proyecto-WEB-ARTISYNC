package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias para MessageFilterServiceImpl.
 * Verifica la detección de teléfonos y correos electrónicos (RF-15).
 */
@ExtendWith(MockitoExtension.class)
class MessageFilterServiceImplTest {

    @InjectMocks
    private MessageFilterServiceImpl filterService;

    // =========================================================================
    // containsContactInfo — Teléfonos
    // =========================================================================

    @Test
    @DisplayName("Detecta número con prefijo internacional +593")
    void contieneContacto_telefonoInternacional_retornaTrue() {
        assertThat(filterService.containsContactInfo("Llámame al +593 99 123 4567")).isTrue();
    }

    @Test
    @DisplayName("Detecta número con código de área (02)")
    void contieneContacto_codigoArea_retornaTrue() {
        assertThat(filterService.containsContactInfo("Mi número fijo es (02) 234567")).isTrue();
    }

    @Test
    @DisplayName("Detecta número en formato 123-456-7890")
    void contieneContacto_formatoNorteamericano_retornaTrue() {
        assertThat(filterService.containsContactInfo("Te llamo al 098-765-4321")).isTrue();
    }

    // =========================================================================
    // containsContactInfo — Correos
    // =========================================================================

    @Test
    @DisplayName("Detecta correo electrónico estándar")
    void contieneContacto_emailEstandar_retornaTrue() {
        assertThat(filterService.containsContactInfo("Escríbeme a usuario@dominio.com")).isTrue();
    }

    @Test
    @DisplayName("Detecta correo con subdominio")
    void contieneContacto_emailSubdominio_retornaTrue() {
        assertThat(filterService.containsContactInfo("Mi correo: nombre@mail.empresa.com")).isTrue();
    }

    // =========================================================================
    // containsContactInfo — Mensajes limpios
    // =========================================================================

    @Test
    @DisplayName("Message sin datos de contacto retorna false")
    void contieneContacto_mensajeLimpio_retornaFalse() {
        assertThat(filterService.containsContactInfo("Hola, ¿puedes enviarme el diseño esta semana?")).isFalse();
    }

    @Test
    @DisplayName("Texto vacío retorna false sin excepción")
    void contieneContacto_textoVacio_retornaFalse() {
        assertThat(filterService.containsContactInfo("")).isFalse();
        assertThat(filterService.containsContactInfo(null)).isFalse();
    }

    // =========================================================================
    // detectPattern
    // =========================================================================

    @Test
    @DisplayName("detectPattern retorna EMAIL para correo electrónico")
    void detectarPatron_email_retornaEMAIL() {
        assertThat(filterService.detectPattern("test@example.com")).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("detectPattern retorna TELEFONO para número de teléfono")
    void detectarPatron_telefono_retornaTELEFONO() {
        assertThat(filterService.detectPattern("+593 99 999 9999")).isEqualTo("TELEFONO");
    }

    @Test
    @DisplayName("detectPattern retorna DESCONOCIDO para texto sin contactos")
    void detectarPatron_sinContacto_retornaDESCONOCIDO() {
        assertThat(filterService.detectPattern("Buen día")).isEqualTo("DESCONOCIDO");
    }
}
