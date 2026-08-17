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
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaColaVerificacion;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaVerificacion;
import uteq.edu.ec.artisync.entity.perfil.CertificadoIa;
import uteq.edu.ec.artisync.entity.perfil.EstadoVerificacion;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.perfil.TipoDocumentoVerificacion;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.exception.ExcepcionServicioIaNoDisponible;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.repository.perfil.EstadoVerificacionRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.perfil.VerificacionColaProyeccion;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;
import uteq.edu.ec.artisync.service.shared.almacenamiento.PrefijoAlmacenamiento;
import uteq.edu.ec.artisync.service.shared.ia.IaService;
import uteq.edu.ec.artisync.service.shared.imagen.PreprocesadorImagenIa;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificacionServicioImplTest {

    @Mock private PerfilCreadorRepository perfilCreadorRepository;
    @Mock private EstadoVerificacionRepository estadoVerificacionRepository;
    @Mock private CertificadoIaRepository certificadoIaRepository;
    @Mock private AlmacenamientoDocumentos almacenamiento;
    @Mock private PreprocesadorImagenIa preprocesador;
    @Mock private IaService iaService;
    @Mock private jakarta.persistence.EntityManager entityManager;

    // Construcción manual, no @InjectMocks: VerificacionServicioImpl toma un
    // ObjectMapper real de Jackson 3 (Tarea 16) que no tiene sentido mockear.
    private VerificacionServicioImpl servicio;

    private PerfilCreador perfil;
    private EstadoVerificacion pendiente;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder().idUsuario(1L).nombres("Ana").apellidos("Creadora").build();
        perfil = PerfilCreador.builder().idPerfil(5L).usuario(usuario).build();
        pendiente = EstadoVerificacion.builder().idEstadoVerificacion(1L).nombreEstado("PENDIENTE").build();
        servicio = new VerificacionServicioImpl(perfilCreadorRepository, estadoVerificacionRepository,
                certificadoIaRepository, almacenamiento, preprocesador, iaService,
                new tools.jackson.databind.ObjectMapper(), entityManager);
    }

    @Test
    void subir_creaSolicitudPendiente_yNuncaLlamaALaIa() {
        MockMultipartFile documento = new MockMultipartFile("documento", "cedula.jpg", "image/jpeg", "contenido".getBytes());
        when(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfil));
        when(estadoVerificacionRepository.findByNombreEstado("PENDIENTE")).thenReturn(Optional.of(pendiente));
        when(almacenamiento.guardar(documento, PrefijoAlmacenamiento.VERIFICACION))
                .thenReturn("verificacion/uuid-generado.jpg");
        when(certificadoIaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RespuestaVerificacion respuesta = servicio.subir(1L, TipoDocumentoVerificacion.IDENTIDAD, documento);

        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("PENDIENTE");
        assertThat(respuesta.tipoDocumento()).isEqualTo("IDENTIDAD");
        verify(preprocesador).validarFormato(documento);
        verifyNoInteractions(iaService);
        // El prefijo no es cosmético: es lo que hace que el router deje estas
        // cédulas en el volumen local en vez de subirlas a Azure.
        verify(almacenamiento).guardar(documento, PrefijoAlmacenamiento.VERIFICACION);
        verify(almacenamiento, never()).guardar(documento);
    }

    @Test
    void subir_usuarioSinPerfilDeCreador_lanza404() {
        MockMultipartFile documento = new MockMultipartFile("documento", "c.jpg", "image/jpeg", "x".getBytes());
        when(perfilCreadorRepository.findByUsuarioIdUsuario(99L)).thenReturn(Optional.empty());

        assertThrows(ExcepcionRecursoNoEncontrado.class,
                () -> servicio.subir(99L, TipoDocumentoVerificacion.IDENTIDAD, documento));
        verifyNoInteractions(iaService, almacenamiento);
    }

    @Test
    void subir_seedDeEstadosAusente_lanza422EnVezDeCrearEstadoAlVuelo() {
        MockMultipartFile documento = new MockMultipartFile("documento", "c.jpg", "image/jpeg", "x".getBytes());
        when(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfil));
        when(estadoVerificacionRepository.findByNombreEstado("PENDIENTE")).thenReturn(Optional.empty());

        assertThrows(ExcepcionReglaNegocio.class,
                () -> servicio.subir(1L, TipoDocumentoVerificacion.IDENTIDAD, documento));
        verify(estadoVerificacionRepository, never()).save(any());
    }

    @Test
    void subir_yaExisteVerificacionPendienteParaElPerfil_lanzaExcepcionReglaNegocio_yNoTocaArchivosNiGuarda() {
        MockMultipartFile documento = new MockMultipartFile("documento", "c.jpg", "image/jpeg", "x".getBytes());
        when(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfil));
        when(certificadoIaRepository.existsByPerfilIdPerfilAndEstadoVerificacionNombreEstado(5L, "PENDIENTE"))
                .thenReturn(true);

        assertThrows(ExcepcionReglaNegocio.class,
                () -> servicio.subir(1L, TipoDocumentoVerificacion.IDENTIDAD, documento));

        verifyNoInteractions(almacenamiento, preprocesador, iaService);
        verify(certificadoIaRepository, never()).save(any());
        verify(estadoVerificacionRepository, never()).findByNombreEstado(any());
    }

    @Test
    void analizarConIa_dictamenAprobado_persisteVeredictoPeroNoElEstado() {
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(10L).perfil(perfil).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(10L)).thenReturn(Optional.of(certificado));
        when(almacenamiento.leer("ref.jpg")).thenReturn("bytes-originales".getBytes());
        when(preprocesador.comprimirParaIa(any())).thenReturn("bytes-comprimidos".getBytes());
        when(iaService.verificarIdentidad(any(), eq("image/jpeg"))).thenReturn(
                IaVerificacionResponse.builder().aprobado(true).confianza(new BigDecimal("0.9"))
                        .nombreDetectado("Ana Pérez").build());
        when(certificadoIaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RespuestaVerificacion respuesta = servicio.analizarConIa(10L);

        assertThat(respuesta.veredictoIa()).isEqualTo("SUGIERE_APROBAR");
        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("PENDIENTE"); // candado del diseño
        assertThat(respuesta.datosExtraidosIa()).contains("Ana Pérez");
    }

    @Test
    void analizarConIa_documentoYaEliminado_lanzaExcepcionReglaNegocio() {
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(11L).perfil(perfil).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(true).build();
        when(certificadoIaRepository.findById(11L)).thenReturn(Optional.of(certificado));

        assertThrows(ExcepcionReglaNegocio.class, () -> servicio.analizarConIa(11L));
        verifyNoInteractions(iaService);
    }

    @Test
    void analizarConIa_iaFalla_noPersisteNada() {
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(12L).perfil(perfil).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(12L)).thenReturn(Optional.of(certificado));
        when(almacenamiento.leer("ref.jpg")).thenReturn("bytes".getBytes());
        when(preprocesador.comprimirParaIa(any())).thenReturn("bytes".getBytes());
        when(iaService.verificarIdentidad(any(), any()))
                .thenThrow(new ExcepcionServicioIaNoDisponible("timeout", null));

        assertThrows(ExcepcionServicioIaNoDisponible.class, () -> servicio.analizarConIa(12L));
        verify(certificadoIaRepository, never()).save(any());
    }

    @Test
    void analizarConIa_tipoCertificado_llamaAlMetodoDeCertificadoNoDeIdentidad() {
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(13L).perfil(perfil).estadoVerificacion(pendiente)
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
        EstadoVerificacion aprobado = EstadoVerificacion.builder().idEstadoVerificacion(2L).nombreEstado("APROBADO").build();
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(20L).perfil(perfil).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(20L)).thenReturn(Optional.of(certificado));
        when(estadoVerificacionRepository.findById(2L)).thenReturn(Optional.of(aprobado));
        doAnswer(inv -> {
            certificado.setEstadoVerificacion(aprobado);
            certificado.setModerador(Usuario.builder().idUsuario(99L).build());
            certificado.setDocumentoEliminado(true);
            return null;
        }).when(entityManager).refresh(certificado);

        RespuestaVerificacion respuesta = servicio.registrarDecision(20L, 99L, 2L, "Documento verificado");

        verify(certificadoIaRepository).registrarDecision(20L, 2L, 99L, "Documento verificado");
        verify(entityManager).refresh(certificado);
        verify(almacenamiento).eliminar("ref.jpg");
        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("APROBADO");
        assertThat(respuesta.idModerador()).isEqualTo(99L);
    }

    @Test
    void registrarDecision_requiereAclaracion_noBorraElDocumento() {
        EstadoVerificacion requiereAclaracion = EstadoVerificacion.builder()
                .idEstadoVerificacion(4L).nombreEstado("REQUIERE_ACLARACION").build();
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(22L).perfil(perfil).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").documentoEliminado(false).build();
        when(certificadoIaRepository.findById(22L)).thenReturn(Optional.of(certificado));
        when(estadoVerificacionRepository.findById(4L)).thenReturn(Optional.of(requiereAclaracion));
        doAnswer(inv -> {
            certificado.setEstadoVerificacion(requiereAclaracion);
            certificado.setModerador(Usuario.builder().idUsuario(99L).build());
            certificado.setDocumentoEliminado(false); // el SP no marca documento_eliminado para este estado
            return null;
        }).when(entityManager).refresh(certificado);

        RespuestaVerificacion respuesta = servicio.registrarDecision(22L, 99L, 4L, "Falta el reverso del documento");

        verify(certificadoIaRepository).registrarDecision(22L, 4L, 99L, "Falta el reverso del documento");
        verify(entityManager).refresh(certificado);
        verify(almacenamiento, never()).eliminar(any());
        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("REQUIERE_ACLARACION");
    }

    @Test
    void registrarDecision_certificadoInexistente_lanza404_yNoLlamaAlProcedimiento() {
        when(certificadoIaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ExcepcionRecursoNoEncontrado.class,
                () -> servicio.registrarDecision(999L, 99L, 2L, "nota"));
        verify(certificadoIaRepository, never()).registrarDecision(any(), any(), any(), any());
    }

    @Test
    void registrarDecision_estadoInexistente_lanza404_yNoLlamaAlProcedimiento() {
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(21L).perfil(perfil).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.findById(21L)).thenReturn(Optional.of(certificado));
        when(estadoVerificacionRepository.findById(777L)).thenReturn(Optional.empty());

        assertThrows(ExcepcionRecursoNoEncontrado.class,
                () -> servicio.registrarDecision(21L, 99L, 777L, "nota"));
        verify(certificadoIaRepository, never()).registrarDecision(any(), any(), any(), any());
    }

    @Test
    void listarCola_delegaEnElRepositorioYMapeaLaProyeccion() {
        VerificacionColaProyeccion fila = mock(VerificacionColaProyeccion.class);
        when(fila.getIdCertificado()).thenReturn(30L);
        when(fila.getNombreEstado()).thenReturn("PENDIENTE");
        when(fila.getNombreCreador()).thenReturn("Ana Creadora");
        when(certificadoIaRepository.listarCola("PENDIENTE", 20, 0)).thenReturn(List.of(fila));

        List<RespuestaColaVerificacion> resultado = servicio.listarCola("PENDIENTE", 20, 0);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).idCertificado()).isEqualTo(30L);
        assertThat(resultado.get(0).nombreCreador()).isEqualTo("Ana Creadora");
    }

    @Test
    void obtenerPorId_revisor_puedeVerCualquierVerificacion() {
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(31L).perfil(perfil).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.findById(31L)).thenReturn(Optional.of(certificado));

        RespuestaVerificacion respuesta = servicio.obtenerPorId(31L, 999L, true);

        assertThat(respuesta.idCertificado()).isEqualTo(31L);
    }

    @Test
    void obtenerPorId_dueno_puedeVerLaSuya() {
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(32L).perfil(perfil).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.findById(32L)).thenReturn(Optional.of(certificado));

        RespuestaVerificacion respuesta = servicio.obtenerPorId(32L, 1L, false); // perfil.usuario.idUsuario == 1L

        assertThat(respuesta.idCertificado()).isEqualTo(32L);
    }

    @Test
    void obtenerPorId_usuarioAjenoSinPermisoDeRevisor_esRechazado() {
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(33L).perfil(perfil).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref.jpg").tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.findById(33L)).thenReturn(Optional.of(certificado));

        assertThrows(AccessDeniedException.class, () -> servicio.obtenerPorId(33L, 777L, false));
    }

    @Test
    void obtenerDocumento_delegaEnElAlmacenamiento() {
        CertificadoIa certificado = CertificadoIa.builder()
                .idCertificado(34L).perfil(perfil).estadoVerificacion(pendiente)
                .urlDocumentoS3("ref-34.jpg").tipoDocumento("IDENTIDAD").build();
        when(certificadoIaRepository.findById(34L)).thenReturn(Optional.of(certificado));
        when(almacenamiento.leer("ref-34.jpg")).thenReturn("contenido".getBytes());

        byte[] resultado = servicio.obtenerDocumento(34L);

        assertThat(new String(resultado)).isEqualTo("contenido");
    }
}
