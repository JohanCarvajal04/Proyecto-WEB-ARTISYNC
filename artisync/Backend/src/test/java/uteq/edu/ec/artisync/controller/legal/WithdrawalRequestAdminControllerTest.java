package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.legal.WithdrawalRequestFilter;
import uteq.edu.ec.artisync.dto.peticion.legal.WithdrawalDecisionRequest;
import uteq.edu.ec.artisync.dto.respuesta.legal.WithdrawalRequestResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IWithdrawalRequestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalRequestAdminControllerTest {

    @Mock
    private IWithdrawalRequestService solicitudRetiroServicio;

    @InjectMocks
    private WithdrawalRequestAdminController controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(900L);
        return user;
    }

    @Test
    void listar_devuelveOk() {
        WithdrawalRequestFilter filtro = new WithdrawalRequestFilter();
        var pageable = PageRequest.of(0, 20);
        Page<WithdrawalRequestResponse> pagina = Page.empty();
        when(solicitudRetiroServicio.listarCola(filtro, pageable)).thenReturn(pagina);

        ResponseEntity<Page<WithdrawalRequestResponse>> res = controlador.listar(filtro, pageable);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(pagina);
    }

    @Test
    void aprobar_devuelveOkYDelegaConIdDelAdminAutenticado() {
        CustomUserDetails user = mockUserDetails();
        WithdrawalRequestResponse respuesta = WithdrawalRequestResponse.builder().idSolicitud(1L).estado("Aprobado").build();
        when(solicitudRetiroServicio.aprobar(1L, 900L)).thenReturn(respuesta);

        ResponseEntity<WithdrawalRequestResponse> res = controlador.aprobar(1L, user);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void rechazar_devuelveOkYPropagaLaNota() {
        CustomUserDetails user = mockUserDetails();
        WithdrawalDecisionRequest peticion = new WithdrawalDecisionRequest();
        peticion.setNotaAdmin("No cumple los requisitos");
        WithdrawalRequestResponse respuesta = WithdrawalRequestResponse.builder().idSolicitud(1L).estado("Rechazado").build();
        when(solicitudRetiroServicio.rechazar(1L, 900L, "No cumple los requisitos")).thenReturn(respuesta);

        ResponseEntity<WithdrawalRequestResponse> res = controlador.rechazar(1L, user, peticion);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
        verify(solicitudRetiroServicio).rechazar(1L, 900L, "No cumple los requisitos");
    }

    @Test
    void reintentar_devuelveOkYDelegaConIdDelAdminAutenticado() {
        CustomUserDetails user = mockUserDetails();
        WithdrawalRequestResponse respuesta = WithdrawalRequestResponse.builder().idSolicitud(1L).estado("Pagado").build();
        when(solicitudRetiroServicio.reintentar(1L, 900L)).thenReturn(respuesta);

        ResponseEntity<WithdrawalRequestResponse> res = controlador.reintentar(1L, user);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }
}
