package uteq.edu.ec.artisync.controller.perfil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionDecisionVerificacion;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaColaVerificacion;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaVerificacion;
import uteq.edu.ec.artisync.entity.perfil.TipoDocumentoVerificacion;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificacionControladorTest {

    @Mock private IVerificacionServicio verificacionServicio;

    @InjectMocks
    private VerificacionControlador controlador;

    private CustomUserDetails usuarioRevisor() {
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("CERTIFICADO_REVISAR"));
        return new CustomUserDetails(99L, "mod@test.dev", "x", true, true, true, true, authorities);
    }

    private CustomUserDetails usuarioCreador() {
        return new CustomUserDetails(1L, "creador@test.dev", "x", true, true, true, true, List.of());
    }

    @Test
    void subir_devuelveCreated() {
        MockMultipartFile documento = new MockMultipartFile("documento", "c.jpg", "image/jpeg", "x".getBytes());
        RespuestaVerificacion respuesta = RespuestaVerificacion.builder().idCertificado(1L).build();
        when(verificacionServicio.subir(1L, TipoDocumentoVerificacion.IDENTIDAD, documento)).thenReturn(respuesta);

        ResponseEntity<RespuestaVerificacion> resultado =
                controlador.subir(TipoDocumentoVerificacion.IDENTIDAD, documento, usuarioCreador());

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void listarCola_devuelveOk() {
        when(verificacionServicio.listarCola("PENDIENTE", 20, 0)).thenReturn(List.of());

        ResponseEntity<List<RespuestaColaVerificacion>> resultado = controlador.listarCola("PENDIENTE", 20, 0);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void analizarConIa_devuelveOk() {
        RespuestaVerificacion respuesta = RespuestaVerificacion.builder().idCertificado(5L).build();
        when(verificacionServicio.analizarConIa(5L)).thenReturn(respuesta);

        ResponseEntity<RespuestaVerificacion> resultado = controlador.analizarConIa(5L);

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody().idCertificado()).isEqualTo(5L);
    }

    @Test
    void registrarDecision_pasaElIdDelModeradorAutenticado() {
        PeticionDecisionVerificacion peticion = new PeticionDecisionVerificacion(2L, "ok");
        RespuestaVerificacion respuesta = RespuestaVerificacion.builder().idCertificado(7L).idModerador(99L).build();
        when(verificacionServicio.registrarDecision(7L, 99L, 2L, "ok")).thenReturn(respuesta);

        ResponseEntity<RespuestaVerificacion> resultado =
                controlador.registrarDecision(7L, peticion, usuarioRevisor());

        assertThat(resultado.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resultado.getBody().idModerador()).isEqualTo(99L);
    }
}
