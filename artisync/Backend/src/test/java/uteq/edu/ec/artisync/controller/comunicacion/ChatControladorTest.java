package uteq.edu.ec.artisync.controller.comunicacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaMensajeChat;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSalaChat;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatControladorTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void obtenerMensajes_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        Pageable pageable = mock(Pageable.class);
        Page<RespuestaMensajeChat> page = new PageImpl<>(Collections.emptyList());
        when(chatService.obtenerMensajes(10L, 1L, pageable)).thenReturn(page);

        ResponseEntity<Page<RespuestaMensajeChat>> res = controlador.obtenerMensajes(10L, pageable, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(page);
    }

    @Test
    void enviarMensaje_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        PeticionEnviarMensaje peticion = new PeticionEnviarMensaje();
        peticion.setCuerpoMensaje("hola");
        RespuestaMensajeChat respuesta = new RespuestaMensajeChat();
        when(chatService.enviarMensaje(10L, 1L, "hola")).thenReturn(respuesta);

        ResponseEntity<RespuestaMensajeChat> res = controlador.enviarMensaje(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerEstado_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaSalaChat respuesta = new RespuestaSalaChat();
        when(chatService.obtenerEstadoSala(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaSalaChat> res = controlador.obtenerEstado(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void enviarMensajeWs_sinIdPedido_lanzaExcepcion() {
        CustomUserDetails user = mockUserDetails();
        PeticionEnviarMensaje peticion = new PeticionEnviarMensaje();
        peticion.setCuerpoMensaje("hola");
        // idPedido es null

        assertThrows(ExcepcionReglaNegocio.class, () -> controlador.enviarMensajeWs(peticion, user));
    }

    @Test
    void enviarMensajeWs_conIdPedido_llamaAServicio() {
        CustomUserDetails user = mockUserDetails();
        PeticionEnviarMensaje peticion = new PeticionEnviarMensaje();
        peticion.setIdPedido(10L);
        peticion.setCuerpoMensaje("hola");

        controlador.enviarMensajeWs(peticion, user);
        org.mockito.Mockito.verify(chatService).enviarMensaje(10L, 1L, "hola");
    }
}
