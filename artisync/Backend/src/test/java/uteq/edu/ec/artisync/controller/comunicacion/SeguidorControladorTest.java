package uteq.edu.ec.artisync.controller.comunicacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaCreadorSeguidoNovedad;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoSeguimiento;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.ISeguidorServicio;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeguidorControladorTest {

    @Mock
    private ISeguidorServicio seguidorServicio;

    @InjectMocks
    private SeguidorControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void seguirCreador_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaEstadoSeguimiento respuesta = new RespuestaEstadoSeguimiento(true, 10L, true);
        when(seguidorServicio.seguirCreador(1L, 10L)).thenReturn(respuesta);

        ResponseEntity<RespuestaEstadoSeguimiento> res = controlador.seguirCreador(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void dejarDeSeguirCreador_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaEstadoSeguimiento respuesta = new RespuestaEstadoSeguimiento(false, 9L, false);
        when(seguidorServicio.dejarDeSeguirCreador(1L, 10L)).thenReturn(respuesta);

        ResponseEntity<RespuestaEstadoSeguimiento> res = controlador.dejarDeSeguirCreador(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerEstadoSeguimiento_conUsuario_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaEstadoSeguimiento respuesta = new RespuestaEstadoSeguimiento(true, 10L, true);
        when(seguidorServicio.obtenerEstadoSeguimiento(1L, 10L)).thenReturn(respuesta);

        ResponseEntity<RespuestaEstadoSeguimiento> res = controlador.obtenerEstadoSeguimiento(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerEstadoSeguimiento_sinUsuario_devuelveOk() {
        RespuestaEstadoSeguimiento respuesta = new RespuestaEstadoSeguimiento(false, 10L, false);
        when(seguidorServicio.obtenerEstadoSeguimiento(null, 10L)).thenReturn(respuesta);

        ResponseEntity<RespuestaEstadoSeguimiento> res = controlador.obtenerEstadoSeguimiento(10L, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarSeguidores_devuelveOk() {
        List<RespuestaSeguidor> lista = Collections.emptyList();
        when(seguidorServicio.listarSeguidores(10L)).thenReturn(lista);

        ResponseEntity<List<RespuestaSeguidor>> res = controlador.listarSeguidores(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarCreadoresSeguidosNovedades_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<RespuestaCreadorSeguidoNovedad> lista = Collections.emptyList();
        when(seguidorServicio.listarCreadoresSeguidosNovedades(1L)).thenReturn(lista);

        ResponseEntity<List<RespuestaCreadorSeguidoNovedad>> res = controlador.listarCreadoresSeguidosNovedades(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void actualizarPortadaYTitulo_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        ResponseEntity<RespuestaMensaje> res = controlador.actualizarPortadaYTitulo("url", "titulo", user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(seguidorServicio).actualizarPortadaYTitulo(1L, "url", "titulo");
    }
}
