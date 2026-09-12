package uteq.edu.ec.artisync.service.perfil.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreateAiCertificateRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.AiCertificateResponse;
import uteq.edu.ec.artisync.entity.perfil.AiCertificate;
import uteq.edu.ec.artisync.entity.perfil.VerificationStatus;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.perfil.AiCertificateRepository;
import uteq.edu.ec.artisync.repository.perfil.VerificationStatusRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiCertificateServiceImplTest {

    @Mock private AiCertificateRepository certificadoRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private VerificationStatusRepository estadoRepository;

    @InjectMocks
    private AiCertificateServiceImpl certificadoIaServicio;

    private User usuario;
    private VerificationStatus estado;
    private AiCertificate certificado;

    @BeforeEach
    void setUp() {
        usuario = User.builder().idUsuario(1L).build();
        estado = VerificationStatus.builder().idEstadoVerificacion(2L).nombreEstado("Pendiente").build();
        certificado = AiCertificate.builder().idCertificado(10L).usuario(usuario).estadoVerificacion(estado)
                .urlDocumentoS3("s3://doc.pdf").puntajeConfianzaIa(new BigDecimal("0.90")).build();
    }

    @Test
    @DisplayName("issueCertificate guarda cuando el usuario y el estado existen")
    void emitirCertificado_guarda() {
        CreateAiCertificateRequest peticion = new CreateAiCertificateRequest(1L, 2L, "s3://doc.pdf", new BigDecimal("0.90"));
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(estadoRepository.findById(2L)).willReturn(Optional.of(estado));
        given(certificadoRepository.save(any(AiCertificate.class))).willAnswer(inv -> inv.getArgument(0));

        AiCertificateResponse respuesta = certificadoIaServicio.issueCertificate(peticion);

        assertThat(respuesta.urlDocumentoS3()).isEqualTo("s3://doc.pdf");
        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("Pendiente");
    }

    @Test
    @DisplayName("issueCertificate lanza recurso no encontrado si el usuario no existe")
    void emitirCertificado_usuarioInexistente() {
        CreateAiCertificateRequest peticion = new CreateAiCertificateRequest(1L, 2L, "s3://doc.pdf", null);
        given(usuarioRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> certificadoIaServicio.issueCertificate(peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("issueCertificate lanza recurso no encontrado si el estado no existe")
    void emitirCertificado_estadoInexistente() {
        CreateAiCertificateRequest peticion = new CreateAiCertificateRequest(1L, 2L, "s3://doc.pdf", null);
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(usuario));
        given(estadoRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> certificadoIaServicio.issueCertificate(peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getCertificateById devuelve el certificado existente")
    void obtenerCertificadoPorId_devuelve() {
        given(certificadoRepository.findById(10L)).willReturn(Optional.of(certificado));

        assertThat(certificadoIaServicio.getCertificateById(10L).idCertificado()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getCertificateById lanza recurso no encontrado si no existe")
    void obtenerCertificadoPorId_inexistente() {
        given(certificadoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> certificadoIaServicio.getCertificateById(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listCertificatesByUser mapea los certificados del usuario")
    void listarCertificadosPorUsuario_mapea() {
        given(certificadoRepository.findByUsuarioIdUsuario(1L)).willReturn(List.of(certificado));

        assertThat(certificadoIaServicio.listCertificatesByUser(1L)).hasSize(1);
    }

    @Test
    @DisplayName("listAllCertificates mapea todos los registros")
    void listarTodosLosCertificados_mapea() {
        given(certificadoRepository.findAll()).willReturn(List.of(certificado));

        assertThat(certificadoIaServicio.listAllCertificates()).hasSize(1);
    }

    @Test
    @DisplayName("deleteCertificate borra cuando existe")
    void eliminarCertificado_borraCuandoExiste() {
        given(certificadoRepository.existsById(10L)).willReturn(true);

        certificadoIaServicio.deleteCertificate(10L);

        verify(certificadoRepository).deleteById(10L);
    }

    @Test
    @DisplayName("deleteCertificate lanza recurso no encontrado si no existe")
    void eliminarCertificado_inexistente() {
        given(certificadoRepository.existsById(10L)).willReturn(false);

        assertThatThrownBy(() -> certificadoIaServicio.deleteCertificate(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
