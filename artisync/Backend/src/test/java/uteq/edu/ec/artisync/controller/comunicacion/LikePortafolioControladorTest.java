package uteq.edu.ec.artisync.controller.comunicacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoLike;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.LikePortafolioService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LikePortafolioControladorTest {

    @Mock
    private LikePortafolioService likeService;

    @InjectMocks
    private LikePortafolioControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void darLike_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        RespuestaEstadoLike respuesta = new RespuestaEstadoLike(1L, 10L, true);
        when(likeService.darLike(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaEstadoLike> res = controlador.darLike(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void quitarLike_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaEstadoLike respuesta = new RespuestaEstadoLike(1L, 9L, false);
        when(likeService.quitarLike(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaEstadoLike> res = controlador.quitarLike(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerEstado_conUsuario_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaEstadoLike respuesta = new RespuestaEstadoLike(1L, 10L, true);
        when(likeService.obtenerEstado(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaEstadoLike> res = controlador.obtenerEstado(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerEstado_sinUsuario_devuelveOk() {
        RespuestaEstadoLike respuesta = new RespuestaEstadoLike(1L, 10L, false);
        when(likeService.obtenerEstado(10L, null)).thenReturn(respuesta);

        ResponseEntity<RespuestaEstadoLike> res = controlador.obtenerEstado(10L, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }
}
