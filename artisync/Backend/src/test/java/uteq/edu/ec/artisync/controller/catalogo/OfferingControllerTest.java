package uteq.edu.ec.artisync.controller.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.catalogo.UpdateAttributeRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.UpdateOfferingRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateAttributeRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateOfferingRequest;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.AttributeResponse;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.OfferingResponse;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.OfferingSummaryResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.service.catalogo.IOfferingCatalogService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferingControllerTest {

    @Mock
    private IOfferingCatalogService servicioCatalogoServicio;

    @InjectMocks
    private OfferingController controlador;

    @Test
    void crearServicio_devuelveCreated() {
        CreateOfferingRequest peticion = new CreateOfferingRequest();
        OfferingResponse respuesta = new OfferingResponse();
        when(servicioCatalogoServicio.crearServicio(10L, peticion)).thenReturn(respuesta);

        ResponseEntity<OfferingResponse> res = controlador.crearServicio(10L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarServicio_devuelveOk() {
        UpdateOfferingRequest peticion = new UpdateOfferingRequest();
        OfferingResponse respuesta = new OfferingResponse();
        when(servicioCatalogoServicio.actualizarServicio(10L, peticion)).thenReturn(respuesta);

        ResponseEntity<OfferingResponse> res = controlador.actualizarServicio(10L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerServicioPorId_devuelveOk() {
        OfferingResponse respuesta = new OfferingResponse();
        when(servicioCatalogoServicio.obtenerServicioPorId(10L)).thenReturn(respuesta);

        ResponseEntity<OfferingResponse> res = controlador.obtenerServicioPorId(10L);
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
        List<OfferingSummaryResponse> lista = Collections.emptyList();
        when(servicioCatalogoServicio.listarServiciosPorCreador(10L, "ACTIVO")).thenReturn(lista);

        ResponseEntity<List<OfferingSummaryResponse>> res = controlador.listarServiciosPorCreador(10L, "ACTIVO");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarAtributosPorServicio_devuelveOk() {
        List<AttributeResponse> lista = Collections.emptyList();
        when(servicioCatalogoServicio.listarAtributosPorServicio(10L)).thenReturn(lista);

        ResponseEntity<List<AttributeResponse>> res = controlador.listarAtributosPorServicio(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void agregarAtributo_devuelveCreated() {
        CreateAttributeRequest peticion = new CreateAttributeRequest();
        AttributeResponse respuesta = new AttributeResponse();
        when(servicioCatalogoServicio.agregarAtributo(10L, peticion)).thenReturn(respuesta);

        ResponseEntity<AttributeResponse> res = controlador.agregarAtributo(10L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarAtributo_devuelveOk() {
        UpdateAttributeRequest peticion = new UpdateAttributeRequest();
        AttributeResponse respuesta = new AttributeResponse();
        when(servicioCatalogoServicio.actualizarAtributo(10L, 20L, peticion)).thenReturn(respuesta);

        ResponseEntity<AttributeResponse> res = controlador.actualizarAtributo(10L, 20L, peticion);
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
