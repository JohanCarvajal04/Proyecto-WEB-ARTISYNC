package uteq.edu.ec.artisync.controller.perfil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearCertificadoIa;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaCertificadoIa;
import uteq.edu.ec.artisync.service.perfil.ICertificadoIaServicio;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificadoIaControladorTest {

    @Mock private ICertificadoIaServicio certificadoServicio;

    @InjectMocks
    private CertificadoIaControlador controlador;

    @Test
    void emitirCertificado_exigeRevisorOAdmin() throws NoSuchMethodException {
        // Antes exigía solo hasRole('ADMIN'): un rol con CERTIFICADO_REVISAR
        // (p. ej. MODERADOR) veía la pantalla de certificados pero no podía
        // emitir uno. Los otros tres métodos del controlador ya usaban el
        // permiso; este quedó desalineado.
        Method metodo = CertificadoIaControlador.class.getMethod("emitirCertificado", PeticionCrearCertificadoIa.class);
        var preAuthorize = metodo.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("CERTIFICADO_REVISAR");
    }

    @Test
    void obtenerCertificadoPorId_exigeRevisorOAdmin() throws NoSuchMethodException {
        Method metodo = CertificadoIaControlador.class.getMethod("obtenerCertificadoPorId", Long.class);
        var preAuthorize = metodo.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("CERTIFICADO_REVISAR");
    }

    @Test
    void listarCertificadosPorUsuario_exigeRevisorOAdmin() throws NoSuchMethodException {
        Method metodo = CertificadoIaControlador.class.getMethod("listarCertificadosPorUsuario", Long.class);
        var preAuthorize = metodo.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).contains("CERTIFICADO_REVISAR");
    }

    @Test
    void emitirCertificado_siguePropagandoLaRespuestaDelServicio() {
        PeticionCrearCertificadoIa peticion = new PeticionCrearCertificadoIa(5L, 1L, "ref.jpg", new BigDecimal("0.9"));
        RespuestaCertificadoIa respuesta = RespuestaCertificadoIa.builder().idCertificado(1L).build();
        when(certificadoServicio.emitirCertificado(peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaCertificadoIa> resultado = controlador.emitirCertificado(peticion);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
