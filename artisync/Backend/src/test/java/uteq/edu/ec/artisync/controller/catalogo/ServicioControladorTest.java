package uteq.edu.ec.artisync.controller.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarAtributo;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarServicio;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearAtributo;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearServicio;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaAtributo;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicio;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaServicioResumido;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.service.catalogo.IServicioCatalogoServicio;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServicioControladorTest {

    @Mock
    private IServicioCatalogoServicio servicioCatalogoServicio;

    @InjectMocks
    private ServicioControlador controlador;

    @Test
    void crearServicio_devuelveCreated() {
        PeticionCrearServicio peticion = new PeticionCrearServicio();
        RespuestaServicio respuesta = new RespuestaServicio();
        when(servicioCatalogoServicio.crearServicio(10L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaServicio> res = controlador.crearServicio(10L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarServicio_devuelveOk() {
        PeticionActualizarServicio peticion = new PeticionActualizarServicio();
        RespuestaServicio respuesta = new RespuestaServicio();
        when(servicioCatalogoServicio.actualizarServicio(10L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaServicio> res = controlador.actualizarServicio(10L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerServicioPorId_devuelveOk() {
        RespuestaServicio respuesta = new RespuestaServicio();
        when(servicioCatalogoServicio.obtenerServicioPorId(10L)).thenReturn(respuesta);

        ResponseEntity<RespuestaServicio> res = controlador.obtenerServicioPorId(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarServicio_devuelveOk() {
        ResponseEntity<RespuestaMensaje> res = controlador.eliminarServicio(10L);
        verify(servicioCatalogoServicio).eliminarServicio(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getMensaje()).contains("eliminado exitosamente");
    }

    @Test
    void listarServiciosPorCreador_devuelveOk() {
        List<RespuestaServicioResumido> lista = Collections.emptyList();
        when(servicioCatalogoServicio.listarServiciosPorCreador(10L, "ACTIVO")).thenReturn(lista);

        ResponseEntity<List<RespuestaServicioResumido>> res = controlador.listarServiciosPorCreador(10L, "ACTIVO");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarAtributosPorServicio_devuelveOk() {
        List<RespuestaAtributo> lista = Collections.emptyList();
        when(servicioCatalogoServicio.listarAtributosPorServicio(10L)).thenReturn(lista);

        ResponseEntity<List<RespuestaAtributo>> res = controlador.listarAtributosPorServicio(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void agregarAtributo_devuelveCreated() {
        PeticionCrearAtributo peticion = new PeticionCrearAtributo();
        RespuestaAtributo respuesta = new RespuestaAtributo();
        when(servicioCatalogoServicio.agregarAtributo(10L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaAtributo> res = controlador.agregarAtributo(10L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarAtributo_devuelveOk() {
        PeticionActualizarAtributo peticion = new PeticionActualizarAtributo();
        RespuestaAtributo respuesta = new RespuestaAtributo();
        when(servicioCatalogoServicio.actualizarAtributo(10L, 20L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaAtributo> res = controlador.actualizarAtributo(10L, 20L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarAtributo_devuelveOk() {
        ResponseEntity<RespuestaMensaje> res = controlador.eliminarAtributo(10L, 20L);
        verify(servicioCatalogoServicio).eliminarAtributo(10L, 20L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getMensaje()).contains("eliminado exitosamente");
    }
}
