package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaCreadorSeguidoNovedad;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoSeguimiento;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;
import uteq.edu.ec.artisync.entity.comunicacion.Seguidor;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.comunicacion.SeguidorRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SeguidorServicioImplTest {

    @Mock
    private SeguidorRepository seguidorRepository;

    @Mock
    private PerfilCreadorRepository perfilCreadorRepository;

    @InjectMocks
    private SeguidorServicioImpl seguidorServicio;

    private PerfilCreador perfilCreador;
    private User usuarioCreador;

    @BeforeEach
    void setUp() {
        usuarioCreador = User.builder()
                .idUsuario(10L)
                .nombres("Valentina")
                .apellidos("Ríos")
                .correo("valentina@artisync.com")
                .build();

        perfilCreador = PerfilCreador.builder()
                .idPerfil(1L)
                .usuario(usuarioCreador)
                .biografia("Ilustradora & Directora de Arte")
                .build();
    }

    @Test
    @DisplayName("seguirCreador — exito cuando usuario no es el creador")
    void seguirCreador_exito() {
        given(perfilCreadorRepository.findById(1L)).willReturn(Optional.of(perfilCreador));
        given(seguidorRepository.ejecutarFnConteoSeguidores(1L)).willReturn(12400L);

        RespuestaEstadoSeguimiento respuesta = seguidorServicio.seguirCreador(20L, 1L);

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getEsSeguidor()).isTrue();
        assertThat(respuesta.getTotalSeguidores()).isEqualTo(12400L);
        assertThat(respuesta.getEsPropioPerfil()).isFalse();

        verify(seguidorRepository).ejecutarFnSeguirCreador(20L, 1L);
    }

    @Test
    @DisplayName("seguirCreador — lanza excepcion si el creador se intenta seguir a si mismo")
    void seguirCreador_autoSeguimiento_lanzaExcepcion() {
        given(perfilCreadorRepository.findById(1L)).willReturn(Optional.of(perfilCreador));

        assertThatThrownBy(() -> seguidorServicio.seguirCreador(10L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no puede seguirse a sí mismo");
    }

    @Test
    @DisplayName("seguirCreador — lanza excepcion si el perfil no existe")
    void seguirCreador_perfilNoExiste_lanzaExcepcion() {
        given(perfilCreadorRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> seguidorServicio.seguirCreador(20L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("dejarDeSeguirCreador — exito")
    void dejarDeSeguirCreador_exito() {
        given(perfilCreadorRepository.findById(1L)).willReturn(Optional.of(perfilCreador));
        given(seguidorRepository.ejecutarFnConteoSeguidores(1L)).willReturn(12399L);

        RespuestaEstadoSeguimiento respuesta = seguidorServicio.dejarDeSeguirCreador(20L, 1L);

        assertThat(respuesta.getEsSeguidor()).isFalse();
        assertThat(respuesta.getTotalSeguidores()).isEqualTo(12399L);

        verify(seguidorRepository).ejecutarFnDejarDeSeguirCreador(20L, 1L);
    }

    @Test
    @DisplayName("obtenerEstadoSeguimiento — detecta perfil propio")
    void obtenerEstadoSeguimiento_esPropioPerfil() {
        given(perfilCreadorRepository.findById(1L)).willReturn(Optional.of(perfilCreador));
        given(seguidorRepository.ejecutarFnConteoSeguidores(1L)).willReturn(12400L);

        RespuestaEstadoSeguimiento respuesta = seguidorServicio.obtenerEstadoSeguimiento(10L, 1L);

        assertThat(respuesta.getEsPropioPerfil()).isTrue();
        assertThat(respuesta.getEsSeguidor()).isFalse();
        assertThat(respuesta.getTotalSeguidores()).isEqualTo(12400L);
    }

    @Test
    @DisplayName("dejarDeSeguirCreador — lanza excepcion si el perfil no existe")
    void dejarDeSeguirCreador_perfilNoExiste_lanzaExcepcion() {
        given(perfilCreadorRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> seguidorServicio.dejarDeSeguirCreador(20L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("obtenerEstadoSeguimiento — visitante autenticado que si es seguidor")
    void obtenerEstadoSeguimiento_visitanteEsSeguidor() {
        given(perfilCreadorRepository.findById(1L)).willReturn(Optional.of(perfilCreador));
        given(seguidorRepository.ejecutarFnEsSeguidor(20L, 1L)).willReturn(Boolean.TRUE);
        given(seguidorRepository.ejecutarFnConteoSeguidores(1L)).willReturn(12400L);

        RespuestaEstadoSeguimiento respuesta = seguidorServicio.obtenerEstadoSeguimiento(20L, 1L);

        assertThat(respuesta.getEsPropioPerfil()).isFalse();
        assertThat(respuesta.getEsSeguidor()).isTrue();
    }

    @Test
    @DisplayName("obtenerEstadoSeguimiento — visitante autenticado que no es seguidor")
    void obtenerEstadoSeguimiento_visitanteNoEsSeguidor() {
        given(perfilCreadorRepository.findById(1L)).willReturn(Optional.of(perfilCreador));
        given(seguidorRepository.ejecutarFnEsSeguidor(20L, 1L)).willReturn(Boolean.FALSE);
        given(seguidorRepository.ejecutarFnConteoSeguidores(1L)).willReturn(12400L);

        RespuestaEstadoSeguimiento respuesta = seguidorServicio.obtenerEstadoSeguimiento(20L, 1L);

        assertThat(respuesta.getEsSeguidor()).isFalse();
    }

    @Test
    @DisplayName("obtenerEstadoSeguimiento — visitante anonimo (sin usuario autenticado)")
    void obtenerEstadoSeguimiento_visitanteAnonimo() {
        given(perfilCreadorRepository.findById(1L)).willReturn(Optional.of(perfilCreador));
        given(seguidorRepository.ejecutarFnConteoSeguidores(1L)).willReturn(12400L);

        RespuestaEstadoSeguimiento respuesta = seguidorServicio.obtenerEstadoSeguimiento(null, 1L);

        assertThat(respuesta.getEsPropioPerfil()).isFalse();
        assertThat(respuesta.getEsSeguidor()).isFalse();
        verify(seguidorRepository, org.mockito.Mockito.never()).ejecutarFnEsSeguidor(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("obtenerEstadoSeguimiento — total nulo se reporta como cero")
    void obtenerEstadoSeguimiento_totalNulo_seReportaComoCero() {
        given(perfilCreadorRepository.findById(1L)).willReturn(Optional.of(perfilCreador));
        given(seguidorRepository.ejecutarFnConteoSeguidores(1L)).willReturn(null);

        RespuestaEstadoSeguimiento respuesta = seguidorServicio.obtenerEstadoSeguimiento(null, 1L);

        assertThat(respuesta.getTotalSeguidores()).isEqualTo(0L);
    }

    @Test
    @DisplayName("listarSeguidores — mapea la lista de seguidores del perfil")
    void listarSeguidores_mapeaLista() {
        User seguidor = User.builder().idUsuario(30L).nombres("Carlos").apellidos("Pino").build();
        Seguidor s = Seguidor.builder()
                .idSeguimiento(5L).usuarioSeguidor(seguidor).perfilCreador(perfilCreador)
                .notificacionesActivas(true).fechaSeguimiento(LocalDateTime.now()).build();
        given(seguidorRepository.findByPerfilCreadorIdPerfil(1L)).willReturn(List.of(s));

        List<RespuestaSeguidor> resultado = seguidorServicio.listarSeguidores(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombreSeguidor()).isEqualTo("Carlos Pino");
        assertThat(resultado.get(0).getIdPerfilCreador()).isEqualTo(1L);
    }

    @Test
    @DisplayName("listarCreadoresSeguidosNovedades — genera el handle a partir del nombre")
    void listarCreadoresSeguidosNovedades_generaHandle() {
        Seguidor s = Seguidor.builder()
                .idSeguimiento(5L).usuarioSeguidor(User.builder().idUsuario(20L).build())
                .perfilCreador(perfilCreador).fechaSeguimiento(LocalDateTime.now()).build();
        given(seguidorRepository.findByUsuarioSeguidorIdUsuario(20L)).willReturn(List.of(s));

        List<RespuestaCreadorSeguidoNovedad> resultado = seguidorServicio.listarCreadoresSeguidosNovedades(20L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getHandle()).isEqualTo("@valentina");
        assertThat(resultado.get(0).getNombreCreador()).isEqualTo("Valentina Ríos");
    }

    @Test
    @DisplayName("listarCreadoresSeguidosNovedades — usa handle por defecto si el creador no tiene nombre")
    void listarCreadoresSeguidosNovedades_sinNombre_usaHandlePorDefecto() {
        User creadorSinNombre = User.builder().idUsuario(11L).build();
        PerfilCreador perfilSinNombre = PerfilCreador.builder().idPerfil(2L).usuario(creadorSinNombre).build();
        Seguidor s = Seguidor.builder()
                .idSeguimiento(6L).usuarioSeguidor(User.builder().idUsuario(20L).build())
                .perfilCreador(perfilSinNombre).fechaSeguimiento(LocalDateTime.now()).build();
        given(seguidorRepository.findByUsuarioSeguidorIdUsuario(20L)).willReturn(List.of(s));

        List<RespuestaCreadorSeguidoNovedad> resultado = seguidorServicio.listarCreadoresSeguidosNovedades(20L);

        assertThat(resultado.get(0).getHandle()).isEqualTo("@creador");
    }

    @Test
    @DisplayName("actualizarPortadaYTitulo — lanza excepcion si el usuario no tiene perfil de creador")
    void actualizarPortadaYTitulo_sinPerfil_lanzaExcepcion() {
        given(perfilCreadorRepository.findByUsuarioIdUsuario(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> seguidorServicio.actualizarPortadaYTitulo(99L, "url", "titulo"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("actualizarPortadaYTitulo — actualiza ambos campos cuando vienen informados")
    void actualizarPortadaYTitulo_actualizaAmbosCampos() {
        given(perfilCreadorRepository.findByUsuarioIdUsuario(10L)).willReturn(Optional.of(perfilCreador));

        boolean resultado = seguidorServicio.actualizarPortadaYTitulo(10L, "https://cdn/portada.jpg", "Ilustradora Senior");

        assertThat(resultado).isTrue();
        assertThat(perfilCreador.getUrlPortada()).isEqualTo("https://cdn/portada.jpg");
        assertThat(perfilCreador.getTituloProfesional()).isEqualTo("Ilustradora Senior");
        verify(perfilCreadorRepository).save(perfilCreador);
    }

    @Test
    @DisplayName("actualizarPortadaYTitulo — no sobrescribe campos que llegan nulos")
    void actualizarPortadaYTitulo_camposNulos_noSobrescribe() {
        perfilCreador.setUrlPortada("https://cdn/original.jpg");
        perfilCreador.setTituloProfesional("Original");
        given(perfilCreadorRepository.findByUsuarioIdUsuario(10L)).willReturn(Optional.of(perfilCreador));

        seguidorServicio.actualizarPortadaYTitulo(10L, null, null);

        assertThat(perfilCreador.getUrlPortada()).isEqualTo("https://cdn/original.jpg");
        assertThat(perfilCreador.getTituloProfesional()).isEqualTo("Original");
    }
}
