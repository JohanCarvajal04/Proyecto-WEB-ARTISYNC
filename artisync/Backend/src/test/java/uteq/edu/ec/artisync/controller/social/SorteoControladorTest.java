package uteq.edu.ec.artisync.controller.social;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionActualizarSorteo;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearSorteo;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaGanador;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaParticipante;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaSorteo;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.social.SorteoService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SorteoControladorTest {

    @Mock
    private SorteoService sorteoService;

    @InjectMocks
    private SorteoControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void crearSorteo_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        PeticionCrearSorteo peticion = new PeticionCrearSorteo();
        RespuestaSorteo respuesta = new RespuestaSorteo();
        when(sorteoService.crearSorteo(1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaSorteo> res = controlador.crearSorteo(peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerSorteo_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaSorteo respuesta = new RespuestaSorteo();
        when(sorteoService.obtenerSorteo(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaSorteo> res = controlador.obtenerSorteo(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerSorteo_sinUser_devuelveOk() {
        RespuestaSorteo respuesta = new RespuestaSorteo();
        when(sorteoService.obtenerSorteo(10L, null)).thenReturn(respuesta);

        ResponseEntity<RespuestaSorteo> res = controlador.obtenerSorteo(10L, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarSorteo_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        PeticionActualizarSorteo peticion = new PeticionActualizarSorteo();
        RespuestaSorteo respuesta = new RespuestaSorteo();
        when(sorteoService.actualizarSorteo(10L, 1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaSorteo> res = controlador.actualizarSorteo(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarSorteo_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaMensaje respuesta = new RespuestaMensaje("Ok");
        when(sorteoService.eliminarSorteo(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaMensaje> res = controlador.eliminarSorteo(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarSorteosPorCreador_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<RespuestaSorteo> lista = Collections.emptyList();
        when(sorteoService.listarSorteosPorCreador(20L, 1L)).thenReturn(lista);

        ResponseEntity<List<RespuestaSorteo>> res = controlador.listarSorteosPorCreador(20L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarSorteosPorCreador_sinUser_devuelveOk() {
        List<RespuestaSorteo> lista = Collections.emptyList();
        when(sorteoService.listarSorteosPorCreador(20L, null)).thenReturn(lista);

        ResponseEntity<List<RespuestaSorteo>> res = controlador.listarSorteosPorCreador(20L, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarSorteosActivos_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<RespuestaSorteo> lista = Collections.emptyList();
        when(sorteoService.listarSorteosActivos(1L)).thenReturn(lista);

        ResponseEntity<List<RespuestaSorteo>> res = controlador.listarSorteosActivos(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarSorteosActivos_sinUser_devuelveOk() {
        List<RespuestaSorteo> lista = Collections.emptyList();
        when(sorteoService.listarSorteosActivos(null)).thenReturn(lista);

        ResponseEntity<List<RespuestaSorteo>> res = controlador.listarSorteosActivos(null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void participar_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        RespuestaParticipante respuesta = new RespuestaParticipante();
        when(sorteoService.participar(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaParticipante> res = controlador.participar(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void cancelarParticipacion_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaMensaje respuesta = new RespuestaMensaje("Ok");
        when(sorteoService.cancelarParticipacion(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaMensaje> res = controlador.cancelarParticipacion(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarParticipantes_devuelveOk() {
        List<RespuestaParticipante> lista = Collections.emptyList();
        when(sorteoService.listarParticipantes(10L)).thenReturn(lista);

        ResponseEntity<List<RespuestaParticipante>> res = controlador.listarParticipantes(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarGanadores_devuelveOk() {
        List<RespuestaGanador> lista = Collections.emptyList();
        when(sorteoService.listarGanadores(10L)).thenReturn(lista);

        ResponseEntity<List<RespuestaGanador>> res = controlador.listarGanadores(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }
}
