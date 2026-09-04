package uteq.edu.ec.artisync.controller.perfil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolio;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPortafolio;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolio;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IPortafolioServicio;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortafolioControladorTest {

    @Mock
    private IPortafolioServicio portafolioServicio;

    @InjectMocks
    private PortafolioControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void crearPortafolio_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        PeticionCrearPortafolio peticion = new PeticionCrearPortafolio(1L, true, Collections.emptyMap());
        RespuestaPortafolio respuesta = new RespuestaPortafolio(1L, 1L, null, 0, true, Collections.emptyMap());
        when(portafolioServicio.crearPortafolio(peticion, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPortafolio> res = controlador.crearPortafolio(peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void obtenerPortafolioPorId_devuelveOk() {
        RespuestaPortafolio respuesta = new RespuestaPortafolio(1L, 1L, null, 0, true, Collections.emptyMap());
        when(portafolioServicio.obtenerPortafolioPorId(10L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPortafolio> res = controlador.obtenerPortafolioPorId(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void obtenerPortafolioPorPerfil_devuelveOk() {
        RespuestaPortafolio respuesta = new RespuestaPortafolio(1L, 1L, null, 0, true, Collections.emptyMap());
        when(portafolioServicio.obtenerPortafolioPorPerfil(10L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPortafolio> res = controlador.obtenerPortafolioPorPerfil(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listarPortafolios_devuelveOk() {
        List<RespuestaPortafolio> lista = Collections.emptyList();
        when(portafolioServicio.listarPortafolios()).thenReturn(lista);

        ResponseEntity<List<RespuestaPortafolio>> res = controlador.listarPortafolios();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actualizarPortafolio_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        PeticionActualizarPortafolio peticion = new PeticionActualizarPortafolio(true, Collections.emptyMap());
        RespuestaPortafolio respuesta = new RespuestaPortafolio(1L, 1L, null, 0, true, Collections.emptyMap());
        when(portafolioServicio.actualizarPortafolio(10L, peticion, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPortafolio> res = controlador.actualizarPortafolio(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void registrarVisita_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        ResponseEntity<RespuestaMensaje> res = controlador.registrarVisita(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(portafolioServicio).incrementarVisitas(10L, 1L);
    }

    @Test
    void eliminarPortafolio_devuelveOk() {
        ResponseEntity<RespuestaMensaje> res = controlador.eliminarPortafolio(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(portafolioServicio).eliminarPortafolio(10L);
    }
}
