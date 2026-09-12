package uteq.edu.ec.artisync.controller.social;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.social.UpdateRaffleRequest;
import uteq.edu.ec.artisync.dto.peticion.social.CreateRaffleRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.social.WinnerResponse;
import uteq.edu.ec.artisync.dto.respuesta.social.ParticipantResponse;
import uteq.edu.ec.artisync.dto.respuesta.social.RaffleResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.social.RaffleService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RaffleControllerTest {

    @Mock
    private RaffleService sorteoService;

    @InjectMocks
    private RaffleController controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void crearSorteo_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        CreateRaffleRequest peticion = new CreateRaffleRequest();
        RaffleResponse respuesta = new RaffleResponse();
        when(sorteoService.createRaffle(1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RaffleResponse> res = controlador.createRaffle(peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerSorteo_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RaffleResponse respuesta = new RaffleResponse();
        when(sorteoService.getRaffle(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RaffleResponse> res = controlador.getRaffle(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerSorteo_sinUser_devuelveOk() {
        RaffleResponse respuesta = new RaffleResponse();
        when(sorteoService.getRaffle(10L, null)).thenReturn(respuesta);

        ResponseEntity<RaffleResponse> res = controlador.getRaffle(10L, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarSorteo_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        UpdateRaffleRequest peticion = new UpdateRaffleRequest();
        RaffleResponse respuesta = new RaffleResponse();
        when(sorteoService.updateRaffle(10L, 1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RaffleResponse> res = controlador.updateRaffle(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarSorteo_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaMensaje respuesta = new RespuestaMensaje("Ok");
        when(sorteoService.deleteRaffle(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaMensaje> res = controlador.deleteRaffle(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarSorteosPorCreador_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<RaffleResponse> lista = Collections.emptyList();
        when(sorteoService.listRafflesByCreator(20L, 1L)).thenReturn(lista);

        ResponseEntity<List<RaffleResponse>> res = controlador.listRafflesByCreator(20L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarSorteosPorCreador_sinUser_devuelveOk() {
        List<RaffleResponse> lista = Collections.emptyList();
        when(sorteoService.listRafflesByCreator(20L, null)).thenReturn(lista);

        ResponseEntity<List<RaffleResponse>> res = controlador.listRafflesByCreator(20L, null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarSorteosActivos_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<RaffleResponse> lista = Collections.emptyList();
        when(sorteoService.listActiveRaffles(1L)).thenReturn(lista);

        ResponseEntity<List<RaffleResponse>> res = controlador.listActiveRaffles(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarSorteosActivos_sinUser_devuelveOk() {
        List<RaffleResponse> lista = Collections.emptyList();
        when(sorteoService.listActiveRaffles(null)).thenReturn(lista);

        ResponseEntity<List<RaffleResponse>> res = controlador.listActiveRaffles(null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void participar_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        ParticipantResponse respuesta = new ParticipantResponse();
        when(sorteoService.joinRaffle(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<ParticipantResponse> res = controlador.joinRaffle(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void cancelarParticipacion_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaMensaje respuesta = new RespuestaMensaje("Ok");
        when(sorteoService.cancelParticipation(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaMensaje> res = controlador.cancelParticipation(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarParticipantes_devuelveOk() {
        List<ParticipantResponse> lista = Collections.emptyList();
        when(sorteoService.listParticipants(10L)).thenReturn(lista);

        ResponseEntity<List<ParticipantResponse>> res = controlador.listParticipants(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarGanadores_devuelveOk() {
        List<WinnerResponse> lista = Collections.emptyList();
        when(sorteoService.listWinners(10L)).thenReturn(lista);

        ResponseEntity<List<WinnerResponse>> res = controlador.listWinners(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }
}
