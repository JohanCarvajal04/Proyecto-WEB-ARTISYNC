package uteq.edu.ec.artisync.service.perfil.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaVerificacion;
import uteq.edu.ec.artisync.entity.perfil.EstadoVerificacion;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.perfil.TipoDocumentoVerificacion;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.repository.perfil.EstadoVerificacionRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;
import uteq.edu.ec.artisync.service.shared.ia.IaService;
import uteq.edu.ec.artisync.service.shared.imagen.PreprocesadorImagenIa;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificacionServicioImplTest {

    @Mock private PerfilCreadorRepository perfilCreadorRepository;
    @Mock private EstadoVerificacionRepository estadoVerificacionRepository;
    @Mock private CertificadoIaRepository certificadoIaRepository;
    @Mock private AlmacenamientoDocumentos almacenamiento;
    @Mock private PreprocesadorImagenIa preprocesador;
    @Mock private IaService iaService;

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
                new tools.jackson.databind.ObjectMapper());
    }

    @Test
    void subir_creaSolicitudPendiente_yNuncaLlamaALaIa() {
        MockMultipartFile documento = new MockMultipartFile("documento", "cedula.jpg", "image/jpeg", "contenido".getBytes());
        when(perfilCreadorRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfil));
        when(estadoVerificacionRepository.findByNombreEstado("PENDIENTE")).thenReturn(Optional.of(pendiente));
        when(almacenamiento.guardar(documento)).thenReturn("uuid-generado.jpg");
        when(certificadoIaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RespuestaVerificacion respuesta = servicio.subir(1L, TipoDocumentoVerificacion.IDENTIDAD, documento);

        assertThat(respuesta.nombreEstadoVerificacion()).isEqualTo("PENDIENTE");
        assertThat(respuesta.tipoDocumento()).isEqualTo("IDENTIDAD");
        verify(preprocesador).validarFormato(documento);
        verifyNoInteractions(iaService);
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
}
