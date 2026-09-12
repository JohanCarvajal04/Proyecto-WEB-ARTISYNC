package uteq.edu.ec.artisync.service.perfil.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import uteq.edu.ec.artisync.dto.ia.IaVerificacionResponse;
import uteq.edu.ec.artisync.dto.respuesta.perfil.VerificationQueueResponse;
import uteq.edu.ec.artisync.dto.respuesta.perfil.VerificationResponse;
import uteq.edu.ec.artisync.entity.perfil.AiCertificate;
import uteq.edu.ec.artisync.entity.perfil.VerificationStatus;
import uteq.edu.ec.artisync.entity.perfil.VerificationDocumentType;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.exception.AiServiceUnavailableException;
import uteq.edu.ec.artisync.repository.perfil.AiCertificateRepository;
import uteq.edu.ec.artisync.repository.perfil.VerificationStatusRepository;
import uteq.edu.ec.artisync.repository.perfil.VerificationQueueProjection;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.ia.AiService;
import uteq.edu.ec.artisync.service.shared.imagen.AiImagePreprocessor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceImplTest {

    @Mock private UserRepository usuarioRepository;
    @Mock private VerificationStatusRepository estadoVerificacionRepository;
    @Mock private AiCertificateRepository certificadoIaRepository;
    @Mock private DocumentStorage almacenamiento;
    @Mock private AiImagePreprocessor preprocesador;
    @Mock private AiService iaService;
    @Mock private jakarta.persistence.EntityManager entityManager;

    // Construcción manual, no @InjectMocks: VerificationServiceImpl toma un
    // ObjectMapper real de Jackson 3 (Tarea 16) que no tiene sentido mockear.
    private VerificationServiceImpl servicio;

    private User usuario;
    private VerificationStatus pendiente;

    @BeforeEach
    void setUp() {
        usuario = User.builder().idUsuario(1L).nombres("Ana").apellidos("Creadora").build();
        pendiente = VerificationStatus.builder().idEstadoVerificacion(1L).nombreEstado("PENDIENTE").build();
        servicio = new VerificationServiceImpl(usuarioRepository, estadoVerificacionRepository,
                certificadoIaRepository, almacenamiento, preprocesador, iaService,
                new tools.jackson.databind.ObjectMapper(), entityManager);
    }

    @Test
    void subir_creaSolicitudPendiente_yNuncaLlamaALaIa() {
        MockMultipartFile documento = new MockMultipartFile("documento", "cedula.jpg", "image/jpeg", "contenido".getBytes());
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estadoVerificacionRepository.findByNombreEstado("PENDIENTE")).thenReturn(Optional.of(pendiente));
        when(almacenamiento.guardar(documento)).thenReturn("uuid-generado.jpg");
        when(certificadoIaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VerificationResponse respuesta = servicio.upload(1L, VerificationDocumentType.IDENTIDAD, documento);

        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("PENDIENTE");
        assertThat(respuesta.tipoDocumento()).isEqualTo("IDENTIDAD");
        verify(preprocesador).validarFormato(documento);
        verifyNoInteractions(iaService);
    }

    @Test
    void subir_usuarioInexistente_lanza404() {
        MockMultipartFile documento = new MockMultipartFile("documento", "c.jpg", "image/jpeg", "x".getBytes());
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.upload(99L, VerificationDocumentType.IDENTIDAD, documento));
        verifyNoInteractions(iaService, almacenamiento);
    }

    @Test
    void subir_seedDeEstadosAusente_lanza422EnVezDeCrearEstadoAlVuelo() {
        MockMultipartFile documento = new MockMultipartFile("documento", "c.jpg", "image/jpeg", "x".getBytes());
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(estadoVerificacionRepository.findByNombreEstado("PENDIENTE")).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class,
                () -> servicio.upload(1L, VerificationDocumentType.IDENTIDAD, documento));
        verify(estadoVerificacionRepository, never()).save(any());
    }

    @Test
    void subir_yaExisteVerificacionPendienteParaElUsuario_lanzaExcepcionReglaNegocio_yNoTocaArchivosNiGuarda() {
        MockMultipartFile documento = new MockMultipartFile("documento", "c.jpg", "image/jpeg", "x".getBytes());
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(certificadoIaRepository.existsByUsuarioIdUsuarioAndEstadoVerificacionNombreEstado(1L, "PENDIENTE"))
                .thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> servicio.upload(1L, VerificationDocumentType.IDENTIDAD, documento));

        verifyNoInteractions(almacenamiento, preprocesador, iaService);
        verify(certificadoIaRepository, never()).save(any());
        verify(estadoVerificacionRepository, never()).findByNombreEstado(any());
    }

    @Test
    void analizarConIa_dictamenAprobado_persisteVeredictoPeroNoElEstado() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(10L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(10L)).thenReturn(Optional.of(certificado));
        when(almacenamiento.leer("ref.jpg")).thenReturn("bytes-originales".getBytes());
        when(preprocesador.comprimirParaIa(any())).thenReturn("bytes-comprimidos".getBytes());
        when(iaService.verificarIdentidad(any(), eq("image/jpeg"))).thenReturn(
                IaVerificacionResponse.builder().aprobado(true).confianza(new BigDecimal("0.9"))
                        .nombreDetectado("Ana Pérez").build());
        when(certificadoIaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VerificationResponse respuesta = servicio.analizarConIa(10L);

        assertThat(respuesta.veredictoIa()).isEqualTo("SUGIERE_APROBAR");
        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("PENDIENTE"); // candado del diseño
        assertThat(respuesta.datosExtraidosIa()).contains("Ana Pérez");
    }

    @Test
    void analizarConIa_documentoYaEliminado_lanzaExcepcionReglaNegocio() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(11L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(true).build();
        when(certificadoIaRepository.findById(11L)).thenReturn(Optional.of(certificado));

        assertThrows(BusinessRuleException.class, () -> servicio.analizarConIa(11L));
        verifyNoInteractions(iaService);
    }

    @Test
    void analizarConIa_iaFalla_noPersisteNada() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(12L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(12L)).thenReturn(Optional.of(certificado));
        when(almacenamiento.leer("ref.jpg")).thenReturn("bytes".getBytes());
        when(preprocesador.comprimirParaIa(any())).thenReturn("bytes".getBytes());
        when(iaService.verificarIdentidad(any(), any()))
                .thenThrow(new AiServiceUnavailableException("timeout", null));

        assertThrows(AiServiceUnavailableException.class, () -> servicio.analizarConIa(12L));
        verify(certificadoIaRepository, never()).save(any());
        // No reintentable (constructor de 2 argumentos): un solo intento, sin reintento.
        verify(iaService, times(1)).verificarIdentidad(any(), any());
    }

    @Test
    void analizarConIa_fallaTransitoria_reintentaUnaVezYTienExito() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(14L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(14L)).thenReturn(Optional.of(certificado));
        when(almacenamiento.leer("ref.jpg")).thenReturn("bytes".getBytes());
        when(preprocesador.comprimirParaIa(any())).thenReturn("bytes".getBytes());
        when(iaService.verificarIdentidad(any(), any()))
                .thenThrow(new AiServiceUnavailableException("429", null, true))
                .thenReturn(IaVerificacionResponse.builder()
                        .aprobado(true).confianza(new BigDecimal("0.9")).build());
        when(certificadoIaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        VerificationResponse respuesta = servicio.analizarConIa(14L);

        assertThat(respuesta.veredictoIa()).isEqualTo("SUGIERE_APROBAR");
        verify(iaService, times(2)).verificarIdentidad(any(), any());
    }

    @Test
    void analizarConIa_fallaNoTransitoria_noReintenta() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(15L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(15L)).thenReturn(Optional.of(certificado));
        when(almacenamiento.leer("ref.jpg")).thenReturn("bytes".getBytes());
        when(preprocesador.comprimirParaIa(any())).thenReturn("bytes".getBytes());
        when(iaService.verificarIdentidad(any(), any()))
                .thenThrow(new AiServiceUnavailableException("401", null, false));

        assertThrows(AiServiceUnavailableException.class, () -> servicio.analizarConIa(15L));
        verify(iaService, times(1)).verificarIdentidad(any(), any());
    }

    @Test
    void analizarConIa_tipoCertificado_llamaAlMetodoDeCertificadoNoDeIdentidad() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(13L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("CERTIFICADO").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(13L)).thenReturn(Optional.of(certificado));
        when(almacenamiento.leer("ref.jpg")).thenReturn("bytes".getBytes());
        when(preprocesador.comprimirParaIa(any())).thenReturn("bytes".getBytes());
        when(iaService.analizarCertificado(any(), any())).thenReturn(
                IaVerificacionResponse.builder().aprobado(true).confianza(new BigDecimal("0.8")).build());
        when(certificadoIaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        servicio.analizarConIa(13L);

        verify(iaService).analizarCertificado(any(), eq("image/jpeg"));
        verify(iaService, never()).verificarIdentidad(any(), any());
    }

    @Test
    void registrarDecision_escribeEstadoModeradorYBorraElDocumento() {
        VerificationStatus aprobado = VerificationStatus.builder().idEstadoVerificacion(2L).nombreEstado("APROBADO").build();
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(20L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(20L)).thenReturn(Optional.of(certificado));
        when(estadoVerificacionRepository.findById(2L)).thenReturn(Optional.of(aprobado));
        doAnswer(inv -> {
            certificado.setEstadoVerificacion(aprobado);
            certificado.setModerador(User.builder().idUsuario(99L).build());
            certificado.setDocumentoEliminado(true);
            return null;
        }).when(entityManager).refresh(certificado);

        VerificationResponse respuesta = servicio.recordDecision(20L, 99L, 2L, "Documento verificado");

        verify(certificadoIaRepository).recordDecision(20L, 2L, 99L, "Documento verificado");
        verify(entityManager).refresh(certificado);
        verify(almacenamiento).eliminar("ref.jpg");
        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("APROBADO");
        assertThat(respuesta.idModerador()).isEqualTo(99L);
    }

    @Test
    void registrarDecision_requiereAclaracion_noBorraElDocumento() {
        VerificationStatus requiereAclaracion = VerificationStatus.builder()
                .idEstadoVerificacion(4L).nombreEstado("REQUIERE_ACLARACION").build();
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(22L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(22L)).thenReturn(Optional.of(certificado));
        when(estadoVerificacionRepository.findById(4L)).thenReturn(Optional.of(requiereAclaracion));
        doAnswer(inv -> {
            certificado.setEstadoVerificacion(requiereAclaracion);
            certificado.setModerador(User.builder().idUsuario(99L).build());
            certificado.setDocumentoEliminado(false); // el SP no marca documento_eliminado para este estado
            return null;
        }).when(entityManager).refresh(certificado);

        VerificationResponse respuesta = servicio.recordDecision(22L, 99L, 4L, "Falta el reverso del documento");

        verify(certificadoIaRepository).recordDecision(22L, 4L, 99L, "Falta el reverso del documento");
        verify(entityManager).refresh(certificado);
        verify(almacenamiento, never()).eliminar(any());
        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("REQUIERE_ACLARACION");
    }

    @Test
    void registrarDecision_certificadoInexistente_lanza404_yNoLlamaAlProcedimiento() {
        when(certificadoIaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.recordDecision(999L, 99L, 2L, "nota"));
        verify(certificadoIaRepository, never()).recordDecision(any(), any(), any(), any());
    }

    @Test
    void registrarDecision_estadoInexistente_lanza404_yNoLlamaAlProcedimiento() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(21L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.findById(21L)).thenReturn(Optional.of(certificado));
        when(estadoVerificacionRepository.findById(777L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.recordDecision(21L, 99L, 777L, "nota"));
        verify(certificadoIaRepository, never()).recordDecision(any(), any(), any(), any());
    }

    @Test
    void listarCola_delegaEnElRepositorioYMapeaLaProyeccion() {
        VerificationQueueProjection fila = mock(VerificationQueueProjection.class);
        when(fila.getIdCertificado()).thenReturn(30L);
        when(fila.getNombreEstado()).thenReturn("PENDIENTE");
        when(fila.getNombreUsuario()).thenReturn("Ana Creadora");
        when(certificadoIaRepository.listQueue("PENDIENTE", 20, 0)).thenReturn(List.of(fila));

        List<VerificationQueueResponse> resultado = servicio.listQueue("PENDIENTE", 20, 0);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).idCertificado()).isEqualTo(30L);
        assertThat(resultado.get(0).nombreUsuario()).isEqualTo("Ana Creadora");
    }

    @Test
    void obtenerPorId_revisor_puedeVerCualquierVerificacion() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(31L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.findById(31L)).thenReturn(Optional.of(certificado));

        VerificationResponse respuesta = servicio.getById(31L, 999L, true);

        assertThat(respuesta.idCertificado()).isEqualTo(31L);
    }

    @Test
    void obtenerPorId_dueno_puedeVerLaSuya() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(32L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.findById(32L)).thenReturn(Optional.of(certificado));

        VerificationResponse respuesta = servicio.getById(32L, 1L, false); // usuario.idUsuario == 1L

        assertThat(respuesta.idCertificado()).isEqualTo(32L);
    }

    @Test
    void obtenerPorId_usuarioAjenoSinPermisoDeRevisor_esRechazado() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(33L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.findById(33L)).thenReturn(Optional.of(certificado));

        assertThrows(AccessDeniedException.class, () -> servicio.getById(33L, 777L, false));
    }

    @Test
    void obtenerDocumento_delegaEnElAlmacenamiento() {
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(34L).usuario(usuario).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref-34.jpg").tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.findById(34L)).thenReturn(Optional.of(certificado));
        when(almacenamiento.leer("ref-34.jpg")).thenReturn("contenido".getBytes());

        byte[] resultado = servicio.getDocument(34L);

        assertThat(new String(resultado)).isEqualTo("contenido");
    }

    @Test
    void estaIdentidadVerificada_delegaEnElRepositorio() {
        when(certificadoIaRepository.existsByUsuarioIdUsuarioAndTipoDocumentoAndEstadoVerificacionNombreEstado(
                1L, "IDENTIDAD", "APROBADO")).thenReturn(true);

        assertThat(servicio.isIdentityVerified(1L)).isTrue();
    }

    @Test
    void obtenerEstadoIdentidad_conCertificadoExistente_devuelveEstadoActual() {
        VerificationStatus aprobado = VerificationStatus.builder().idEstadoVerificacion(2L).nombreEstado("APROBADO").build();
        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(1L).usuario(usuario).estadoVerificacion(aprobado)
                .tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.existsByUsuarioIdUsuarioAndTipoDocumentoAndEstadoVerificacionNombreEstado(
                1L, "IDENTIDAD", "APROBADO")).thenReturn(true);
        when(certificadoIaRepository.findTopByUsuarioIdUsuarioAndTipoDocumentoOrderByFechaAnalisisDesc(1L, "IDENTIDAD"))
                .thenReturn(Optional.of(certificado));

        var respuesta = servicio.getIdentityStatus(1L);

        assertThat(respuesta.verificado()).isTrue();
        assertThat(respuesta.estadoActual()).isEqualTo("APROBADO");
    }

    @Test
    void obtenerEstadoIdentidad_sinCertificado_devuelveEstadoActualNulo() {
        when(certificadoIaRepository.existsByUsuarioIdUsuarioAndTipoDocumentoAndEstadoVerificacionNombreEstado(
                1L, "IDENTIDAD", "APROBADO")).thenReturn(false);
        when(certificadoIaRepository.findTopByUsuarioIdUsuarioAndTipoDocumentoOrderByFechaAnalisisDesc(1L, "IDENTIDAD"))
                .thenReturn(Optional.empty());

        var respuesta = servicio.getIdentityStatus(1L);

        assertThat(respuesta.verificado()).isFalse();
        assertThat(respuesta.estadoActual()).isNull();
    }
}
