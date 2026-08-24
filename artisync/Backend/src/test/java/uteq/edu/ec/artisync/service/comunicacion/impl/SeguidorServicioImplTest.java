package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoSeguimiento;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.SeguidorRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;

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
    private Usuario usuarioCreador;

    @BeforeEach
    void setUp() {
        usuarioCreador = Usuario.builder()
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
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("no puede seguirse a sí mismo");
    }

    @Test
    @DisplayName("seguirCreador — lanza excepcion si el perfil no existe")
    void seguirCreador_perfilNoExiste_lanzaExcepcion() {
        given(perfilCreadorRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> seguidorServicio.seguirCreador(20L, 99L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
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
}
