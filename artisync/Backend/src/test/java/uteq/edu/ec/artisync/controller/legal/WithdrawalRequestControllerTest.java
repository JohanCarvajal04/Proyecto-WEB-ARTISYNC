package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.legal.CreateWithdrawalRequest;
import uteq.edu.ec.artisync.dto.respuesta.legal.CreatorBalanceResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.WithdrawalRequestResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IWithdrawalRequestService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalRequestControllerTest {

    @Mock
    private IWithdrawalRequestService solicitudRetiroServicio;

    @InjectMocks
    private WithdrawalRequestController controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(200L);
        return user;
    }

    @Test
    void obtenerSaldo_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        CreatorBalanceResponse saldo = CreatorBalanceResponse.builder()
                .saldoDisponible(new BigDecimal("50.00")).montoMinimoRetiro(new BigDecimal("10.00"))
                .tieneCorreoPaypalConfigurado(true).tieneSolicitudPendiente(false).build();
        when(solicitudRetiroServicio.obtenerSaldo(200L)).thenReturn(saldo);

        ResponseEntity<CreatorBalanceResponse> res = controlador.obtenerSaldo(user);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(saldo);
    }

    @Test
    void solicitar_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        CreateWithdrawalRequest peticion = new CreateWithdrawalRequest();
        peticion.setMontoSolicitado(new BigDecimal("20.00"));
        WithdrawalRequestResponse respuesta = WithdrawalRequestResponse.builder()
                .idSolicitud(1L).estado("Pendiente").build();
        when(solicitudRetiroServicio.solicitar(200L, peticion)).thenReturn(respuesta);

        ResponseEntity<WithdrawalRequestResponse> res = controlador.solicitar(user, peticion);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void misSolicitudes_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        WithdrawalRequestResponse respuesta = WithdrawalRequestResponse.builder().idSolicitud(1L).build();
        when(solicitudRetiroServicio.misSolicitudes(200L)).thenReturn(List.of(respuesta));

        ResponseEntity<List<WithdrawalRequestResponse>> res = controlador.misSolicitudes(user);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsExactly(respuesta);
    }
}
