package uteq.edu.ec.artisync.controller.social;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearResena;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaResena;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.social.ResenaService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResenaControladorTest {

    @Mock
    private ResenaService resenaService;

    @InjectMocks
    private ResenaControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void crearResena_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        PeticionCrearResena peticion = new PeticionCrearResena();
        RespuestaResena respuesta = new RespuestaResena();
        when(resenaService.crearResena(10L, peticion, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaResena> res = controlador.crearResena(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerMiResena_conResena_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaResena respuesta = new RespuestaResena();
        when(resenaService.obtenerMiResena(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaResena> res = controlador.obtenerMiResena(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerMiResena_sinResena_devuelveNotFound() {
        CustomUserDetails user = mockUserDetails();
        when(resenaService.obtenerMiResena(10L, 1L)).thenReturn(null);

        ResponseEntity<RespuestaResena> res = controlador.obtenerMiResena(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void actualizarResena_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        PeticionCrearResena peticion = new PeticionCrearResena();
        RespuestaResena respuesta = new RespuestaResena();
        when(resenaService.actualizarResena(10L, peticion, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaResena> res = controlador.actualizarResena(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarResena_devuelveNoContent() {
        CustomUserDetails user = mockUserDetails();
        controlador.eliminarResena(10L, user);
        verify(resenaService).eliminarResena(10L, 1L);
    }

    @Test
    void listarResenas_devuelveOk() {
        List<RespuestaResena> lista = Collections.emptyList();
        when(resenaService.listarResenasPorCreador(10L)).thenReturn(lista);

        ResponseEntity<List<RespuestaResena>> res = controlador.listarResenas(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void obtenerPromedio_devuelveOk() {
        when(resenaService.calcularPromedioPorCreador(10L)).thenReturn(4.5);

        ResponseEntity<Map<String, Object>> res = controlador.obtenerPromedio(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsEntry("idPerfil", 10L);
        assertThat(res.getBody()).containsEntry("promedio", 4.5);
    }
}
