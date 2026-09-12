package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.legal.CancelPaymentRequest;
import uteq.edu.ec.artisync.dto.respuesta.legal.PaymentResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IPaymentService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private IPaymentService pagoServicio;

    @InjectMocks
    private PaymentController controlador;

    private CustomUserDetails usuario(Long idUsuario) {
        return new CustomUserDetails(idUsuario, "cliente", "x", true, true, true, true, List.of());
    }

    @Test
    void crearOrdenPago_DebeRetornarLaOrdenCreada() {
        CustomUserDetails cliente = usuario(1L);
        PaymentResponse respuesta = PaymentResponse.builder().idPago(10L).approvalUrl("https://paypal/approve").build();
        when(pagoServicio.createPayPalOrder(5L, 1L, null)).thenReturn(respuesta);

        ResponseEntity<PaymentResponse> result = controlador.createPaymentOrder(5L, cliente);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getIdPago()).isEqualTo(10L);
    }

    @Test
    void obtenerEstadoPago_DebeRetornarElEstadoActual() {
        CustomUserDetails usuario = usuario(2L);
        when(pagoServicio.getPaymentStatus(5L, 2L))
                .thenReturn(PaymentResponse.builder().idPago(10L).estadoFondos("RETENIDO").build());

        ResponseEntity<PaymentResponse> result = controlador.getPaymentStatus(5L, usuario);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getEstadoFondos()).isEqualTo("RETENIDO");
    }

    @Test
    void cancelarPago_ConCuerpoInformado_DebeExtraerAccionYMotivo() {
        CustomUserDetails cliente = usuario(1L);
        CancelPaymentRequest peticion = new CancelPaymentRequest();
        peticion.setAccionFondos("LIBERAR");
        peticion.setMotivo("acuerdo mutuo");
        when(pagoServicio.cancelOrderWithHeldFunds(5L, 1L, "LIBERAR", "acuerdo mutuo"))
                .thenReturn(PaymentResponse.builder().idPago(10L).estadoFondos("LIBERADO").build());

        ResponseEntity<PaymentResponse> result = controlador.cancelPayment(5L, cliente, peticion);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getEstadoFondos()).isEqualTo("LIBERADO");
    }

    @Test
    void cancelarPago_SinCuerpo_DebeDelegarConAccionYMotivoNulos() {
        CustomUserDetails cliente = usuario(1L);
        when(pagoServicio.cancelOrderWithHeldFunds(eq(5L), eq(1L), isNull(), isNull()))
                .thenReturn(PaymentResponse.builder().idPago(10L).estadoFondos("REEMBOLSADO").build());

        ResponseEntity<PaymentResponse> result = controlador.cancelPayment(5L, cliente, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getEstadoFondos()).isEqualTo("REEMBOLSADO");
    }
}
