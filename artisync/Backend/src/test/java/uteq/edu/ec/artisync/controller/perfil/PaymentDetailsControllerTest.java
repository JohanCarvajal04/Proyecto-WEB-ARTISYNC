package uteq.edu.ec.artisync.controller.perfil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.perfil.PaymentDetailsRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.PaymentDetailsResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IPaymentDetailsService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentDetailsControllerTest {

    @Mock
    private IPaymentDetailsService datosPagoServicio;

    @InjectMocks
    private PaymentDetailsController controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(200L);
        return user;
    }

    @Test
    void obtenerMisDatosPago_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        PaymentDetailsResponse respuesta = PaymentDetailsResponse.builder()
                .correoPaypal("ana@paypal.test").fechaActualizacion(LocalDateTime.now()).build();
        when(datosPagoServicio.getMyPaymentDetails(200L)).thenReturn(respuesta);

        ResponseEntity<PaymentDetailsResponse> res = controlador.getMyPaymentDetails(user);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarCorreoPaypal_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        PaymentDetailsRequest peticion = new PaymentDetailsRequest();
        peticion.setCorreoPaypal("nuevo@paypal.test");
        PaymentDetailsResponse respuesta = PaymentDetailsResponse.builder()
                .correoPaypal("nuevo@paypal.test").fechaActualizacion(LocalDateTime.now()).build();
        when(datosPagoServicio.updatePaypalEmail(200L, peticion)).thenReturn(respuesta);

        ResponseEntity<PaymentDetailsResponse> res = controlador.updatePaypalEmail(user, peticion);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }
}
