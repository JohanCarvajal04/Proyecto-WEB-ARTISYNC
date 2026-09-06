package uteq.edu.ec.artisync.controller.perfil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionDatosPago;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaDatosPago;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IDatosPagoServicio;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatosPagoControladorTest {

    @Mock
    private IDatosPagoServicio datosPagoServicio;

    @InjectMocks
    private DatosPagoControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(200L);
        return user;
    }

    @Test
    void obtenerMisDatosPago_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaDatosPago respuesta = RespuestaDatosPago.builder()
                .correoPaypal("ana@paypal.test").fechaActualizacion(LocalDateTime.now()).build();
        when(datosPagoServicio.obtenerMisDatosPago(200L)).thenReturn(respuesta);

        ResponseEntity<RespuestaDatosPago> res = controlador.obtenerMisDatosPago(user);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void actualizarCorreoPaypal_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        PeticionDatosPago peticion = new PeticionDatosPago();
        peticion.setCorreoPaypal("nuevo@paypal.test");
        RespuestaDatosPago respuesta = RespuestaDatosPago.builder()
                .correoPaypal("nuevo@paypal.test").fechaActualizacion(LocalDateTime.now()).build();
        when(datosPagoServicio.actualizarCorreoPaypal(200L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaDatosPago> res = controlador.actualizarCorreoPaypal(user, peticion);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }
}
