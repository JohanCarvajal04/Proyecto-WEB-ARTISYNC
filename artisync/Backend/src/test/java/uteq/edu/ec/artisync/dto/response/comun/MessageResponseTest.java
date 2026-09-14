package uteq.edu.ec.artisync.dto.response.comun;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageResponseTest {

    @Test
    void getMessage_devuelveElMismoValorQueMensaje() {
        MessageResponse respuesta = new MessageResponse("Sesión cerrada exitosamente");

        assertEquals("Sesión cerrada exitosamente", respuesta.mensaje());
        assertEquals(respuesta.mensaje(), respuesta.getMessage());
    }
}
