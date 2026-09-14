package uteq.edu.ec.artisync.dto.response.comun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RespuestaUrlTest {

    @Test
    void url_devuelveElValorConstruido() {
        RespuestaUrl respuesta = new RespuestaUrl("https://ejemplo.dev/archivo.pdf");

        assertEquals("https://ejemplo.dev/archivo.pdf", respuesta.url());
    }
}
