package uteq.edu.ec.artisync.controller.catalog;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.request.catalog.UpdateAttributeRequest;
import uteq.edu.ec.artisync.dto.request.catalog.UpdateOfferingRequest;
import uteq.edu.ec.artisync.dto.request.catalog.CreateAttributeRequest;
import uteq.edu.ec.artisync.dto.request.catalog.CreateOfferingRequest;
import uteq.edu.ec.artisync.dto.response.catalog.AttributeResponse;
import uteq.edu.ec.artisync.dto.response.catalog.OfferingResponse;
import uteq.edu.ec.artisync.dto.response.catalog.OfferingSummaryResponse;
import uteq.edu.ec.artisync.dto.response.comun.MessageResponse;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.service.catalog.IOfferingCatalogService;
import uteq.edu.ec.artisync.service.shared.storage.DocumentStorage;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OfferingControllerTest {

    @Mock
    private IOfferingCatalogService servicioCatalogoServicio;

    @Mock
    private DocumentStorage almacenamientoDocumentos;

    @InjectMocks
    private OfferingController controlador;

    @Test
    void crearServicio_devuelveCreated() {
        CreateOfferingRequest peticion = new CreateOfferingRequest();
        OfferingResponse respuesta = new OfferingResponse();
        when(servicioCatalogoServicio.createOffering(10L, peticion)).thenReturn(respuesta);

        ResponseEntity<OfferingResponse> res = controlador.createOffering(10L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarServicio_devuelveOk() {
        UpdateOfferingRequest peticion = new UpdateOfferingRequest();
        OfferingResponse respuesta = new OfferingResponse();
        when(servicioCatalogoServicio.updateOffering(10L, peticion)).thenReturn(respuesta);

        ResponseEntity<OfferingResponse> res = controlador.updateOffering(10L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerServicioPorId_devuelveOk() {
        OfferingResponse respuesta = new OfferingResponse();
        when(servicioCatalogoServicio.getOfferingById(10L)).thenReturn(respuesta);

        ResponseEntity<OfferingResponse> res = controlador.getOfferingById(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarServicio_devuelveOk() {
        ResponseEntity<MessageResponse> res = controlador.deleteOffering(10L);
        verify(servicioCatalogoServicio).deleteOffering(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getMessage()).contains("eliminado exitosamente");
    }

    @Test
    void listarServiciosPorCreador_devuelveOk() {
        List<OfferingSummaryResponse> lista = Collections.emptyList();
        when(servicioCatalogoServicio.listOfferingsByCreator(10L, "ACTIVO")).thenReturn(lista);

        ResponseEntity<List<OfferingSummaryResponse>> res = controlador.listOfferingsByCreator(10L, "ACTIVO");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarAtributosPorServicio_devuelveOk() {
        List<AttributeResponse> lista = Collections.emptyList();
        when(servicioCatalogoServicio.listAttributesByOffering(10L)).thenReturn(lista);

        ResponseEntity<List<AttributeResponse>> res = controlador.listAttributesByOffering(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void agregarAtributo_devuelveCreated() {
        CreateAttributeRequest peticion = new CreateAttributeRequest();
        AttributeResponse respuesta = new AttributeResponse();
        when(servicioCatalogoServicio.addAttribute(10L, peticion)).thenReturn(respuesta);

        ResponseEntity<AttributeResponse> res = controlador.addAttribute(10L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarAtributo_devuelveOk() {
        UpdateAttributeRequest peticion = new UpdateAttributeRequest();
        AttributeResponse respuesta = new AttributeResponse();
        when(servicioCatalogoServicio.updateAttribute(10L, 20L, peticion)).thenReturn(respuesta);

        ResponseEntity<AttributeResponse> res = controlador.updateAttribute(10L, 20L, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarAtributo_devuelveOk() {
        ResponseEntity<MessageResponse> res = controlador.deleteAttribute(10L, 20L);
        verify(servicioCatalogoServicio).deleteAttribute(10L, 20L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getMessage()).contains("eliminado exitosamente");
    }

    @Test
    void subirMiniatura_devuelveCreated() {
        MultipartFile archivo = new MockMultipartFile("archivo", "miniatura.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(servicioCatalogoServicio.uploadThumbnail(archivo)).thenReturn("servicios/uuid.jpg");

        var res = controlador.uploadThumbnail(archivo);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().url()).isEqualTo("servicios/uuid.jpg");
    }

    @Test
    void servirMiniatura_pathInvalido_lanzaExcepcion() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/servicios/miniatura/verificacion/archivo.jpg");

        assertThrows(ResourceNotFoundException.class, () -> controlador.servirMiniatura(request));
    }

    @Test
    void servirMiniatura_pathValido_devuelveOk() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/v1/servicios/miniatura/servicios/uuid.jpg");
        when(almacenamientoDocumentos.read("servicios/uuid.jpg")).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> res = controlador.servirMiniatura(request);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsExactly(1, 2, 3);
        assertThat(res.getHeaders().getContentType().toString()).isEqualTo("image/jpeg");
    }
}
