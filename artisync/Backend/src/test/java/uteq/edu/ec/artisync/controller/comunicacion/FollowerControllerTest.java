package uteq.edu.ec.artisync.controller.comunicacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.FollowedCreatorUpdateResponse;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.FollowStatusResponse;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.FollowerResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.IFollowerService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowerControllerTest {

    @Mock
    private IFollowerService seguidorServicio;

    @InjectMocks
    private FollowerController controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void seguirCreador_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        FollowStatusResponse respuesta = new FollowStatusResponse(true, 10L, true);
        when(seguidorServicio.followCreator(1L, 10L)).thenReturn(respuesta);

        ResponseEntity<FollowStatusResponse> res = controlador.followCreator(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void dejarDeSeguirCreador_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        FollowStatusResponse respuesta = new FollowStatusResponse(false, 9L, false);
        when(seguidorServicio.unfollowCreator(1L, 10L)).thenReturn(respuesta);

        ResponseEntity<FollowStatusResponse> res = controlador.unfollowCreator(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerEstadoSeguimiento_conUsuario_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        FollowStatusResponse respuesta = new FollowStatusResponse(true, 10L, true);
        when(seguidorServicio.getFollowStatus(1L, 10L)).thenReturn(respuesta);

        ResponseEntity<FollowStatusResponse> res = controlador.getFollowStatus(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerEstadoSeguimiento_sinUsuario_devuelveOk() {
        FollowStatusResponse respuesta = new FollowStatusResponse(false, 10L, false);
        when(seguidorServicio.getFollowStatus(null, 10L)).thenReturn(respuesta);

        ResponseEntity<FollowStatusResponse> res = controlador.getFollowStatus(10L, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarSeguidores_devuelveOk() {
        List<FollowerResponse> lista = Collections.emptyList();
        when(seguidorServicio.listFollowers(10L)).thenReturn(lista);

        ResponseEntity<List<FollowerResponse>> res = controlador.listFollowers(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarCreadoresSeguidosNovedades_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<FollowedCreatorUpdateResponse> lista = Collections.emptyList();
        when(seguidorServicio.listFollowedCreatorUpdates(1L)).thenReturn(lista);

        ResponseEntity<List<FollowedCreatorUpdateResponse>> res = controlador.listFollowedCreatorUpdates(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void actualizarPortadaYTitulo_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        ResponseEntity<RespuestaMensaje> res = controlador.updateCoverAndTitle("url", "titulo", user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(seguidorServicio).updateCoverAndTitle(1L, "url", "titulo");
    }
}
