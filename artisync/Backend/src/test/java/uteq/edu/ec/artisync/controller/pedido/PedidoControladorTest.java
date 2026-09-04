package uteq.edu.ec.artisync.controller.pedido;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionAvanzarEtapa;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPedido;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearPropuestaTerminos;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaHistorialEstado;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPedidoResumido;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaPropuestaTerminos;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaSeguimientoPedido;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.IPedidoServicio;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PedidoControladorTest {

    @Mock
    private IPedidoServicio pedidoServicio;

    @InjectMocks
    private PedidoControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void crearPedido_devuelveCreated() {
        PeticionCrearPedido peticion = new PeticionCrearPedido();
        RespuestaPedido respuesta = new RespuestaPedido();
        CustomUserDetails user = mockUserDetails();
        when(pedidoServicio.crearPedido(1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaPedido> res = controlador.crearPedido(user, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerPedido_devuelveOk() {
        RespuestaPedido respuesta = new RespuestaPedido();
        CustomUserDetails user = mockUserDetails();
        when(pedidoServicio.obtenerPedidoPorId(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPedido> res = controlador.obtenerPedido(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarMisPedidos_devuelveOk() {
        List<RespuestaPedidoResumido> lista = Collections.emptyList();
        CustomUserDetails user = mockUserDetails();
        when(pedidoServicio.listarMisPedidos(1L)).thenReturn(lista);

        ResponseEntity<List<RespuestaPedidoResumido>> res = controlador.listarMisPedidos(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarMisComisiones_devuelveOk() {
        List<RespuestaPedidoResumido> lista = Collections.emptyList();
        CustomUserDetails user = mockUserDetails();
        when(pedidoServicio.listarMisComisiones(1L)).thenReturn(lista);

        ResponseEntity<List<RespuestaPedidoResumido>> res = controlador.listarMisComisiones(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void exportarMisPedidos_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        DocumentoGenerado doc = new DocumentoGenerado(new byte[]{1, 2, 3}, "application/pdf", "pedidos.pdf");
        when(pedidoServicio.exportarMisPedidos(1L, FormatoReporte.PDF, "test@test.com")).thenReturn(doc);

        ResponseEntity<byte[]> res = controlador.exportarMisPedidos(user, FormatoReporte.PDF, auth);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void exportarMisComisiones_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        DocumentoGenerado doc = new DocumentoGenerado(new byte[]{1, 2, 3}, "application/pdf", "comisiones.pdf");
        List<Long> ids = List.of(1L, 2L);
        when(pedidoServicio.exportarMisComisiones(1L, ids, FormatoReporte.PDF, "test@test.com")).thenReturn(doc);

        ResponseEntity<byte[]> res = controlador.exportarMisComisiones(user, FormatoReporte.PDF, ids, auth);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void avanzarEtapa_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        PeticionAvanzarEtapa peticion = new PeticionAvanzarEtapa();
        RespuestaPedido respuesta = new RespuestaPedido();
        when(pedidoServicio.avanzarEtapa(10L, 1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaPedido> res = controlador.avanzarEtapa(10L, user, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void proponerTerminos_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        PeticionCrearPropuestaTerminos peticion = new PeticionCrearPropuestaTerminos();
        RespuestaPropuestaTerminos respuesta = new RespuestaPropuestaTerminos();
        when(pedidoServicio.proponerTerminos(10L, 1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaPropuestaTerminos> res = controlador.proponerTerminos(10L, user, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerPropuestaPendiente_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaPropuestaTerminos respuesta = new RespuestaPropuestaTerminos();
        when(pedidoServicio.obtenerPropuestaPendiente(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPropuestaTerminos> res = controlador.obtenerPropuestaPendiente(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void aceptarPropuestaTerminos_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaPedido respuesta = new RespuestaPedido();
        when(pedidoServicio.aceptarPropuestaTerminos(10L, 20L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPedido> res = controlador.aceptarPropuestaTerminos(10L, 20L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void rechazarPropuestaTerminos_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaPropuestaTerminos respuesta = new RespuestaPropuestaTerminos();
        when(pedidoServicio.rechazarPropuestaTerminos(10L, 20L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPropuestaTerminos> res = controlador.rechazarPropuestaTerminos(10L, 20L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void cancelarPropuestaTerminos_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaPropuestaTerminos respuesta = new RespuestaPropuestaTerminos();
        when(pedidoServicio.cancelarPropuestaTerminos(10L, 20L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPropuestaTerminos> res = controlador.cancelarPropuestaTerminos(10L, 20L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerHistorial_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<RespuestaHistorialEstado> lista = Collections.emptyList();
        when(pedidoServicio.obtenerHistorial(10L, 1L)).thenReturn(lista);

        ResponseEntity<List<RespuestaHistorialEstado>> res = controlador.obtenerHistorial(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void obtenerSeguimiento_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaSeguimientoPedido respuesta = new RespuestaSeguimientoPedido();
        when(pedidoServicio.obtenerSeguimiento(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaSeguimientoPedido> res = controlador.obtenerSeguimiento(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void bloquearDeleteHistorial_devuelveForbidden() {
        ResponseEntity<RespuestaMensaje> res = controlador.bloquearDeleteHistorial(1L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void bloquearPatchHistorial_devuelveForbidden() {
        ResponseEntity<RespuestaMensaje> res = controlador.bloquearPatchHistorial(1L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
