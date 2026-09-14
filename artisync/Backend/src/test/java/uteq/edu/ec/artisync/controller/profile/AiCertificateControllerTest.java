package uteq.edu.ec.artisync.controller.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.request.profile.CreateAiCertificateRequest;
import uteq.edu.ec.artisync.dto.response.comun.MessageResponse;
import uteq.edu.ec.artisync.dto.response.profile.AiCertificateResponse;
import uteq.edu.ec.artisync.service.profile.IAiCertificateService;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCertificateControllerTest {

    @Mock private IAiCertificateService certificadoServicio;

    @InjectMocks
    private AiCertificateController controlador;

    @Test
    void emitirCertificado_exigeRevisorOAdmin() throws NoSuchMethodException {
        // Antes exigía solo hasRole('ADMIN'): un rol con CERTIFICADO_REVISAR
        // (p. ej. MODERADOR) veía la pantalla de certificados pero no podía
        // emitir uno. Los otros tres métodos del controlador ya usaban el
        // permiso; este quedó desalineado.
        Method metodo = AiCertificateController.class.getMethod("issueCertificate", CreateAiCertificateRequest.class);
        var preAuthorize = metodo.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("CERTIFICADO_REVISAR");
    }

    @Test
    void obtenerCertificadoPorId_exigeRevisorOAdmin() throws NoSuchMethodException {
        Method metodo = AiCertificateController.class.getMethod("getCertificateById", Long.class);
        var preAuthorize = metodo.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("CERTIFICADO_REVISAR");
    }

    @Test
    void listarCertificadosPorUsuario_exigeRevisorOAdmin() throws NoSuchMethodException {
        Method metodo = AiCertificateController.class.getMethod("listCertificatesByUser", Long.class);
        var preAuthorize = metodo.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("CERTIFICADO_REVISAR");
    }

    @Test
    void emitirCertificado_siguePropagandoLaRespuestaDelServicio() {
        CreateAiCertificateRequest peticion = new CreateAiCertificateRequest(5L, 1L, "ref.jpg", new BigDecimal("0.9"));
        AiCertificateResponse respuesta = AiCertificateResponse.builder().idCertificado(1L).build();
        when(certificadoServicio.issueCertificate(peticion)).thenReturn(respuesta);

        ResponseEntity<AiCertificateResponse> resultado = controlador.issueCertificate(peticion);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void obtenerCertificadoPorId_devuelveElCertificado() {
        AiCertificateResponse certificado = AiCertificateResponse.builder().idCertificado(1L).build();
        when(certificadoServicio.getCertificateById(1L)).thenReturn(certificado);

        ResponseEntity<AiCertificateResponse> resultado = controlador.getCertificateById(1L);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isEqualTo(certificado);
    }

    @Test
    void listarCertificadosPorUsuario_devuelveElListado() {
        List<AiCertificateResponse> listado = List.of(AiCertificateResponse.builder().idCertificado(1L).build());
        when(certificadoServicio.listCertificatesByUser(5L)).thenReturn(listado);

        ResponseEntity<List<AiCertificateResponse>> resultado = controlador.listCertificatesByUser(5L);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isEqualTo(listado);
    }

    @Test
    void listarTodosLosCertificados_devuelveElListadoCompleto() {
        List<AiCertificateResponse> listado = List.of(AiCertificateResponse.builder().idCertificado(1L).build());
        when(certificadoServicio.listAllCertificates()).thenReturn(listado);

        ResponseEntity<List<AiCertificateResponse>> resultado = controlador.listAllCertificates();

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody()).isEqualTo(listado);
    }

    @Test
    void eliminarCertificado_devuelveMensajeDeConfirmacion() {
        ResponseEntity<MessageResponse> resultado = controlador.deleteCertificate(1L);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody().getMessage()).contains("eliminado exitosamente");
    }
}
