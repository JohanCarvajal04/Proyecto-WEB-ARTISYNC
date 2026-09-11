package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.SignatureStatusResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IContractService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractControllerTest {

    @Mock
    private IContractService contratoServicio;

    @InjectMocks
    private ContractController controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void generarContrato_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        ContractResponse respuesta = new ContractResponse();
        when(contratoServicio.generarContrato(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<ContractResponse> res = controlador.generarContrato(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void firmarContrato_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        ContractResponse respuesta = new ContractResponse();
        when(contratoServicio.firmarContrato(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<ContractResponse> res = controlador.firmarContrato(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerContrato_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        ContractResponse respuesta = new ContractResponse();
        when(contratoServicio.obtenerContrato(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<ContractResponse> res = controlador.obtenerContrato(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerContratoPorPedido_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        ContractResponse respuesta = new ContractResponse();
        when(contratoServicio.obtenerContratoPorPedido(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<ContractResponse> res = controlador.obtenerContratoPorPedido(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerEstadoFirma_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        SignatureStatusResponse respuesta = new SignatureStatusResponse(10L, true, false, false, "En proceso");
        when(contratoServicio.obtenerEstadoFirma(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<SignatureStatusResponse> res = controlador.obtenerEstadoFirma(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void descargarPdf_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        byte[] pdf = new byte[]{1, 2, 3};
        when(contratoServicio.generarPdf(10L, 1L)).thenReturn(pdf);

        ResponseEntity<byte[]> res = controlador.descargarPdf(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_PDF);
        assertThat(res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment");
        assertThat(res.getBody()).isEqualTo(pdf);
    }
}
