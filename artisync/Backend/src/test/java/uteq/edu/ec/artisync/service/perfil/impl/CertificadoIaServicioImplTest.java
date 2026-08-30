package uteq.edu.ec.artisync.service.perfil.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearCertificadoIa;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaCertificadoIa;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.entity.perfil.EstadoVerificacion;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.repository.perfil.EstadoVerificacionRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CertificadoIaServicioImplTest {

    @Mock private CertificadoIaRepository certificadoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EstadoVerificacionRepository estadoRepository;

    @InjectMocks
    private CertificadoIaServicioImpl certificadoIaServicio;

    private Usuario usuario;
    private EstadoVerificacion estado;
    private CertificadoIa certificado;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder().idUsuario(1L).build();
        estado = EstadoVerificacion.builder().idEstadoVerificacion(2L).nombreEstado("Pendiente").build();
        certificado = CertificadoIa.builder().idCertificado(10L).usuario(usuario).estadoVerificacion(estado)
                .urlDocumentoS3("s3://doc.pdf").puntajeConfianzaIa(new BigDecimal("0.90")).build();
    }

    @Test
    @DisplayName("emitirCertificado guarda cuando el usuario y el estado existen")
    void emitirCertificado_guarda() {
        PeticionCrearCertificadoIa peticion = new PeticionCrearCertificadoIa(1L, 2L, "s3://doc.pdf", new BigDecimal("0.90"));
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(estadoRepository.findById(2L)).willReturn(Optional.of(estado));
        given(certificadoRepository.save(any(CertificadoIa.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaCertificadoIa respuesta = certificadoIaServicio.emitirCertificado(peticion);

        assertThat(respuesta.urlDocumentoS3()).isEqualTo("s3://doc.pdf");
        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("Pendiente");
    }

    @Test
    @DisplayName("emitirCertificado lanza recurso no encontrado si el usuario no existe")
    void emitirCertificado_usuarioInexistente() {
        PeticionCrearCertificadoIa peticion = new PeticionCrearCertificadoIa(1L, 2L, "s3://doc.pdf", null);
        given(usuarioRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> certificadoIaServicio.emitirCertificado(peticion))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("emitirCertificado lanza recurso no encontrado si el estado no existe")
    void emitirCertificado_estadoInexistente() {
        PeticionCrearCertificadoIa peticion = new PeticionCrearCertificadoIa(1L, 2L, "s3://doc.pdf", null);
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(estadoRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> certificadoIaServicio.emitirCertificado(peticion))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerCertificadoPorId devuelve el certificado existente")
    void obtenerCertificadoPorId_devuelve() {
        given(certificadoRepository.findById(10L)).willReturn(Optional.of(certificado));

        assertThat(certificadoIaServicio.obtenerCertificadoPorId(10L).idCertificado()).isEqualTo(10L);
    }

    @Test
    @DisplayName("obtenerCertificadoPorId lanza recurso no encontrado si no existe")
    void obtenerCertificadoPorId_inexistente() {
        given(certificadoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> certificadoIaServicio.obtenerCertificadoPorId(10L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("listarCertificadosPorUsuario mapea los certificados del usuario")
    void listarCertificadosPorUsuario_mapea() {
        given(certificadoRepository.findByUsuarioIdUsuario(1L)).willReturn(List.of(certificado));

        assertThat(certificadoIaServicio.listarCertificadosPorUsuario(1L)).hasSize(1);
    }

    @Test
    @DisplayName("listarTodosLosCertificados mapea todos los registros")
    void listarTodosLosCertificados_mapea() {
        given(certificadoRepository.findAll()).willReturn(List.of(certificado));

        assertThat(certificadoIaServicio.listarTodosLosCertificados()).hasSize(1);
    }

    @Test
    @DisplayName("eliminarCertificado borra cuando existe")
    void eliminarCertificado_borraCuandoExiste() {
        given(certificadoRepository.existsById(10L)).willReturn(true);

        certificadoIaServicio.eliminarCertificado(10L);

        verify(certificadoRepository).deleteById(10L);
    }

    @Test
    @DisplayName("eliminarCertificado lanza recurso no encontrado si no existe")
    void eliminarCertificado_inexistente() {
        given(certificadoRepository.existsById(10L)).willReturn(false);

        assertThatThrownBy(() -> certificadoIaServicio.eliminarCertificado(10L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }
}
