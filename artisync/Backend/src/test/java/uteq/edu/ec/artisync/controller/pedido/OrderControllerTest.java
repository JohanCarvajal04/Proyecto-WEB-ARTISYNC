package uteq.edu.ec.artisync.controller.pedido;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import uteq.edu.ec.artisync.dto.peticion.pedido.AdvanceStageRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateOrderRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateTermsProposalRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.pedido.StatusHistoryResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderSummaryResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.TermsProposalResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderTrackingResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.IOrderService;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private IOrderService pedidoServicio;

    @InjectMocks
    private OrderController controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void crearPedido_devuelveCreated() {
        CreateOrderRequest peticion = new CreateOrderRequest();
        OrderResponse respuesta = new OrderResponse();
        CustomUserDetails user = mockUserDetails();
        when(pedidoServicio.crearPedido(1L, peticion)).thenReturn(respuesta);

        ResponseEntity<OrderResponse> res = controlador.crearPedido(user, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerPedido_devuelveOk() {
        OrderResponse respuesta = new OrderResponse();
        CustomUserDetails user = mockUserDetails();
        when(pedidoServicio.obtenerPedidoPorId(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<OrderResponse> res = controlador.obtenerPedido(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarMisPedidos_devuelveOk() {
        List<OrderSummaryResponse> lista = Collections.emptyList();
        CustomUserDetails user = mockUserDetails();
        when(pedidoServicio.listarMisPedidos(1L)).thenReturn(lista);

        ResponseEntity<List<OrderSummaryResponse>> res = controlador.listarMisPedidos(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarMisComisiones_devuelveOk() {
        List<OrderSummaryResponse> lista = Collections.emptyList();
        CustomUserDetails user = mockUserDetails();
        when(pedidoServicio.listarMisComisiones(1L)).thenReturn(lista);

        ResponseEntity<List<OrderSummaryResponse>> res = controlador.listarMisComisiones(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void exportarMisPedidos_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        GeneratedDocument doc = new GeneratedDocument(new byte[]{1, 2, 3}, "application/pdf", "pedidos.pdf");
        when(pedidoServicio.exportarMisPedidos(1L, ReportFormat.PDF, "test@test.com")).thenReturn(doc);

        ResponseEntity<byte[]> res = controlador.exportarMisPedidos(user, ReportFormat.PDF, auth);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void exportarMisComisiones_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        GeneratedDocument doc = new GeneratedDocument(new byte[]{1, 2, 3}, "application/pdf", "comisiones.pdf");
        List<Long> ids = List.of(1L, 2L);
        when(pedidoServicio.exportarMisComisiones(1L, ids, ReportFormat.PDF, "test@test.com")).thenReturn(doc);

        ResponseEntity<byte[]> res = controlador.exportarMisComisiones(user, ReportFormat.PDF, ids, auth);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void avanzarEtapa_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        AdvanceStageRequest peticion = new AdvanceStageRequest();
        OrderResponse respuesta = new OrderResponse();
        when(pedidoServicio.avanzarEtapa(10L, 1L, peticion)).thenReturn(respuesta);

        ResponseEntity<OrderResponse> res = controlador.avanzarEtapa(10L, user, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void proponerTerminos_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        CreateTermsProposalRequest peticion = new CreateTermsProposalRequest();
        TermsProposalResponse respuesta = new TermsProposalResponse();
        when(pedidoServicio.proponerTerminos(10L, 1L, peticion)).thenReturn(respuesta);

        ResponseEntity<TermsProposalResponse> res = controlador.proponerTerminos(10L, user, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerPropuestaPendiente_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        TermsProposalResponse respuesta = new TermsProposalResponse();
        when(pedidoServicio.obtenerPropuestaPendiente(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<TermsProposalResponse> res = controlador.obtenerPropuestaPendiente(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void aceptarPropuestaTerminos_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        OrderResponse respuesta = new OrderResponse();
        when(pedidoServicio.aceptarPropuestaTerminos(10L, 20L, 1L)).thenReturn(respuesta);

        ResponseEntity<OrderResponse> res = controlador.aceptarPropuestaTerminos(10L, 20L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void rechazarPropuestaTerminos_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        TermsProposalResponse respuesta = new TermsProposalResponse();
        when(pedidoServicio.rechazarPropuestaTerminos(10L, 20L, 1L)).thenReturn(respuesta);

        ResponseEntity<TermsProposalResponse> res = controlador.rechazarPropuestaTerminos(10L, 20L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void cancelarPropuestaTerminos_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        TermsProposalResponse respuesta = new TermsProposalResponse();
        when(pedidoServicio.cancelarPropuestaTerminos(10L, 20L, 1L)).thenReturn(respuesta);

        ResponseEntity<TermsProposalResponse> res = controlador.cancelarPropuestaTerminos(10L, 20L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerHistorial_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<StatusHistoryResponse> lista = Collections.emptyList();
        when(pedidoServicio.obtenerHistorial(10L, 1L)).thenReturn(lista);

        ResponseEntity<List<StatusHistoryResponse>> res = controlador.obtenerHistorial(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void obtenerSeguimiento_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        OrderTrackingResponse respuesta = new OrderTrackingResponse();
        when(pedidoServicio.obtenerSeguimiento(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<OrderTrackingResponse> res = controlador.obtenerSeguimiento(10L, user);
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
