package uteq.edu.ec.artisync.controller.comunicacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearBriefingPlantilla;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarBriefing;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionResponderBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaBriefing;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.BriefingService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BriefingControladorTest {

    @Mock
    private BriefingService briefingService;

    @InjectMocks
    private BriefingControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void crearPlantilla_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        PeticionCrearBriefingPlantilla peticion = new PeticionCrearBriefingPlantilla();
        RespuestaBriefing respuesta = new RespuestaBriefing();
        when(briefingService.crearPlantilla(1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaBriefing> res = controlador.crearPlantilla(peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerMisPlantillas_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<RespuestaBriefing> lista = Collections.emptyList();
        when(briefingService.obtenerMisPlantillas(1L)).thenReturn(lista);

        ResponseEntity<List<RespuestaBriefing>> res = controlador.obtenerMisPlantillas(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void editarPlantilla_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        PeticionCrearBriefingPlantilla peticion = new PeticionCrearBriefingPlantilla();
        RespuestaBriefing respuesta = new RespuestaBriefing();
        when(briefingService.editarPlantilla(10L, 1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaBriefing> res = controlador.editarPlantilla(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarPlantilla_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaMensaje respuesta = new RespuestaMensaje("Ok");
        when(briefingService.eliminarPlantilla(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaMensaje> res = controlador.eliminarPlantilla(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void enviarBriefing_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        PeticionEnviarBriefing peticion = new PeticionEnviarBriefing();
        RespuestaBriefing respuesta = new RespuestaBriefing();
        when(briefingService.enviarBriefing(10L, peticion, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaBriefing> res = controlador.enviarBriefing(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerBriefing_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaBriefing respuesta = new RespuestaBriefing();
        when(briefingService.obtenerBriefing(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaBriefing> res = controlador.obtenerBriefing(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void responderBriefing_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        PeticionResponderBriefing peticion = new PeticionResponderBriefing();
        RespuestaBriefing respuesta = new RespuestaBriefing();
        when(briefingService.responderBriefing(10L, peticion, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaBriefing> res = controlador.responderBriefing(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }
}
