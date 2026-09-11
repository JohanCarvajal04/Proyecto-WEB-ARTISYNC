package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionCancelarPago;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPago;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IPagoServicio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoControladorTest {

    @Mock
    private IPagoServicio pagoServicio;

    @InjectMocks
    private PagoControlador controlador;

    private CustomUserDetails usuario(Long idUsuario) {
        return new CustomUserDetails(idUsuario, "cliente", "x", true, true, true, true, List.of());
    }

    @Test
    void crearOrdenPago_DebeRetornarLaOrdenCreada() {
        CustomUserDetails cliente = usuario(1L);
        RespuestaPago respuesta = RespuestaPago.builder().idPago(10L).approvalUrl("https://paypal/approve").build();
        when(pagoServicio.crearOrdenPayPal(5L, 1L, null)).thenReturn(respuesta);

        ResponseEntity<RespuestaPago> result = controlador.crearOrdenPago(5L, cliente);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getIdPago()).isEqualTo(10L);
    }

    @Test
    void obtenerEstadoPago_DebeRetornarElEstadoActual() {
        CustomUserDetails usuario = usuario(2L);
        when(pagoServicio.obtenerEstadoPago(5L, 2L))
                .thenReturn(RespuestaPago.builder().idPago(10L).estadoFondos("RETENIDO").build());

        ResponseEntity<RespuestaPago> result = controlador.obtenerEstadoPago(5L, usuario);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getEstadoFondos()).isEqualTo("RETENIDO");
    }

    @Test
    void cancelarPago_ConCuerpoInformado_DebeExtraerAccionYMotivo() {
        CustomUserDetails cliente = usuario(1L);
        PeticionCancelarPago peticion = new PeticionCancelarPago();
        peticion.setAccionFondos("LIBERAR");
        peticion.setMotivo("acuerdo mutuo");
        when(pagoServicio.cancelarPedidoConFondosRetenidos(5L, 1L, "LIBERAR", "acuerdo mutuo"))
                .thenReturn(RespuestaPago.builder().idPago(10L).estadoFondos("LIBERADO").build());

        ResponseEntity<RespuestaPago> result = controlador.cancelarPago(5L, cliente, peticion);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getEstadoFondos()).isEqualTo("LIBERADO");
    }

    @Test
    void cancelarPago_SinCuerpo_DebeDelegarConAccionYMotivoNulos() {
        CustomUserDetails cliente = usuario(1L);
        when(pagoServicio.cancelarPedidoConFondosRetenidos(eq(5L), eq(1L), isNull(), isNull()))
                .thenReturn(RespuestaPago.builder().idPago(10L).estadoFondos("REEMBOLSADO").build());

        ResponseEntity<RespuestaPago> result = controlador.cancelarPago(5L, cliente, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getEstadoFondos()).isEqualTo("REEMBOLSADO");
    }
}
