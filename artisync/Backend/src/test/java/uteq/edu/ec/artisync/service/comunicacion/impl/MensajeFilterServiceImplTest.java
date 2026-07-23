package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas unitarias para MensajeFilterServiceImpl.
 * Verifica la detección de teléfonos y correos electrónicos (RF-15).
 */
@ExtendWith(MockitoExtension.class)
class MensajeFilterServiceImplTest {

    @InjectMocks
    private MensajeFilterServiceImpl filterService;

    // =========================================================================
    // contieneContacto — Teléfonos
    // =========================================================================

    @Test
    @DisplayName("Detecta número con prefijo internacional +593")
    void contieneContacto_telefonoInternacional_retornaTrue() {
        assertThat(filterService.contieneContacto("Llámame al +593 99 123 4567")).isTrue();
    }

    @Test
    @DisplayName("Detecta número con código de área (02)")
    void contieneContacto_codigoArea_retornaTrue() {
        assertThat(filterService.contieneContacto("Mi número fijo es (02) 234567")).isTrue();
    }

    @Test
    @DisplayName("Detecta número en formato 123-456-7890")
    void contieneContacto_formatoNorteamericano_retornaTrue() {
        assertThat(filterService.contieneContacto("Te llamo al 098-765-4321")).isTrue();
    }

    // =========================================================================
    // contieneContacto — Correos
    // =========================================================================

    @Test
    @DisplayName("Detecta correo electrónico estándar")
    void contieneContacto_emailEstandar_retornaTrue() {
        assertThat(filterService.contieneContacto("Escríbeme a usuario@dominio.com")).isTrue();
    }

    @Test
    @DisplayName("Detecta correo con subdominio")
    void contieneContacto_emailSubdominio_retornaTrue() {
        assertThat(filterService.contieneContacto("Mi correo: nombre@mail.empresa.com")).isTrue();
    }

    // =========================================================================
    // contieneContacto — Mensajes limpios
    // =========================================================================

    @Test
    @DisplayName("Mensaje sin datos de contacto retorna false")
    void contieneContacto_mensajeLimpio_retornaFalse() {
        assertThat(filterService.contieneContacto("Hola, ¿puedes enviarme el diseño esta semana?")).isFalse();
    }

    @Test
    @DisplayName("Texto vacío retorna false sin excepción")
    void contieneContacto_textoVacio_retornaFalse() {
        assertThat(filterService.contieneContacto("")).isFalse();
        assertThat(filterService.contieneContacto(null)).isFalse();
    }

    // =========================================================================
    // detectarPatron
    // =========================================================================

    @Test
    @DisplayName("detectarPatron retorna EMAIL para correo electrónico")
    void detectarPatron_email_retornaEMAIL() {
        assertThat(filterService.detectarPatron("test@example.com")).isEqualTo("EMAIL");
    }

    @Test
    @DisplayName("detectarPatron retorna TELEFONO para número de teléfono")
    void detectarPatron_telefono_retornaTELEFONO() {
        assertThat(filterService.detectarPatron("+593 99 999 9999")).isEqualTo("TELEFONO");
    }

    @Test
    @DisplayName("detectarPatron retorna DESCONOCIDO para texto sin contactos")
    void detectarPatron_sinContacto_retornaDESCONOCIDO() {
        assertThat(filterService.detectarPatron("Buen día")).isEqualTo("DESCONOCIDO");
    }
}
