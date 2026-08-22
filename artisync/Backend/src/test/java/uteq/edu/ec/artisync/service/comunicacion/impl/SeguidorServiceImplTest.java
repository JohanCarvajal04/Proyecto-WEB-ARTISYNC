package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;
import uteq.edu.ec.artisync.entity.comunicacion.Seguidor;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.SeguidorRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pruebas unitarias de SeguidorServiceImpl.
 * REQ-F-009: seguir y dejar de seguir a un creador, con contador de efecto inmediato.
 */
@ExtendWith(MockitoExtension.class)
class SeguidorServiceImplTest {

    private static final Long ID_PERFIL = 7L;
    private static final Long ID_SEGUIDOR = 42L;
    private static final Long ID_DUENO_PERFIL = 99L;

    @Mock private SeguidorRepository seguidorRepository;
    @Mock private PerfilCreadorRepository perfilCreadorRepository;
    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private SeguidorServiceImpl seguidorService;

    private PerfilCreador perfil;
    private Usuario seguidor;

    @BeforeEach
    void setUp() {
        Usuario dueno = new Usuario();
        dueno.setIdUsuario(ID_DUENO_PERFIL);

        perfil = new PerfilCreador();
        perfil.setIdPerfil(ID_PERFIL);
        perfil.setUsuario(dueno);

        seguidor = new Usuario();
        seguidor.setIdUsuario(ID_SEGUIDOR);
        seguidor.setNombres("Ana");
        seguidor.setApellidos("Torres");
    }

    private Seguidor seguimientoPersistido() {
        return Seguidor.builder()
                .idSeguimiento(1L)
                .usuarioSeguidor(seguidor)
                .perfilCreador(perfil)
                .notificacionesActivas(true)
                .fechaSeguimiento(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("seguir: registra el seguimiento y devuelve los datos del seguidor")
    void seguir_deberiaRegistrarSeguimiento() {
        given(perfilCreadorRepository.findById(ID_PERFIL)).willReturn(Optional.of(perfil));
        given(seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
                ID_SEGUIDOR, ID_PERFIL)).willReturn(false);
        given(usuarioRepository.findById(ID_SEGUIDOR)).willReturn(Optional.of(seguidor));
        given(seguidorRepository.save(any(Seguidor.class))).willReturn(seguimientoPersistido());

        RespuestaSeguidor respuesta = seguidorService.seguir(ID_PERFIL, ID_SEGUIDOR);

        assertThat(respuesta.getIdSeguimiento()).isEqualTo(1L);
        assertThat(respuesta.getIdUsuarioSeguidor()).isEqualTo(ID_SEGUIDOR);
        assertThat(respuesta.getNombreSeguidor()).isEqualTo("Ana Torres");
        assertThat(respuesta.getIdPerfilCreador()).isEqualTo(ID_PERFIL);
        verify(seguidorRepository).save(any(Seguidor.class));
    }

    @Test
    @DisplayName("seguir: falla si el perfil del creador no existe")
    void seguir_deberiaFallarSiPerfilNoExiste() {
        given(perfilCreadorRepository.findById(ID_PERFIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> seguidorService.seguir(ID_PERFIL, ID_SEGUIDOR))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class)
                .hasMessageContaining("Perfil de creador no encontrado");

        verify(seguidorRepository, never()).save(any());
    }

    @Test
    @DisplayName("seguir: un creador no puede seguir su propio perfil")
    void seguir_deberiaFallarSiSeSigueASiMismo() {
        given(perfilCreadorRepository.findById(ID_PERFIL)).willReturn(Optional.of(perfil));

        assertThatThrownBy(() -> seguidorService.seguir(ID_PERFIL, ID_DUENO_PERFIL))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("tu propio perfil");

        verify(seguidorRepository, never()).save(any());
    }

    @Test
    @DisplayName("seguir: falla si ya sigue al creador")
    void seguir_deberiaFallarSiYaSigue() {
        given(perfilCreadorRepository.findById(ID_PERFIL)).willReturn(Optional.of(perfil));
        given(seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
                ID_SEGUIDOR, ID_PERFIL)).willReturn(true);

        assertThatThrownBy(() -> seguidorService.seguir(ID_PERFIL, ID_SEGUIDOR))
                .isInstanceOf(ExcepcionRecursoDuplicado.class)
                .hasMessageContaining("Ya sigues");

        verify(seguidorRepository, never()).save(any());
    }

    @Test
    @DisplayName("dejarDeSeguir: elimina la relación existente")
    void dejarDeSeguir_deberiaEliminarSeguimiento() {
        Seguidor existente = seguimientoPersistido();
        given(seguidorRepository.findByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
                ID_SEGUIDOR, ID_PERFIL)).willReturn(Optional.of(existente));

        seguidorService.dejarDeSeguir(ID_PERFIL, ID_SEGUIDOR);

        verify(seguidorRepository).delete(existente);
    }

    @Test
    @DisplayName("dejarDeSeguir: falla si no seguía al creador")
    void dejarDeSeguir_deberiaFallarSiNoSigue() {
        given(seguidorRepository.findByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
                ID_SEGUIDOR, ID_PERFIL)).willReturn(Optional.empty());

        assertThatThrownBy(() -> seguidorService.dejarDeSeguir(ID_PERFIL, ID_SEGUIDOR))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class)
                .hasMessageContaining("No sigues");

        verify(seguidorRepository, never()).delete(any());
    }

    @Test
    @DisplayName("contarSeguidores: refleja el total del repositorio")
    void contarSeguidores_deberiaDevolverTotal() {
        given(seguidorRepository.countByPerfilCreadorIdPerfil(ID_PERFIL)).willReturn(3L);

        assertThat(seguidorService.contarSeguidores(ID_PERFIL)).isEqualTo(3L);
    }

    @Test
    @DisplayName("sigue: refleja el estado de seguimiento del usuario")
    void sigue_deberiaDevolverEstado() {
        given(seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
                ID_SEGUIDOR, ID_PERFIL)).willReturn(true);

        assertThat(seguidorService.sigue(ID_PERFIL, ID_SEGUIDOR)).isTrue();
    }

    @Test
    @DisplayName("listarSeguidores: mapea cada seguimiento a su DTO")
    void listarSeguidores_deberiaMapearSeguidores() {
        given(seguidorRepository.findByPerfilCreadorIdPerfil(ID_PERFIL))
                .willReturn(List.of(seguimientoPersistido()));

        List<RespuestaSeguidor> seguidores = seguidorService.listarSeguidores(ID_PERFIL);

        assertThat(seguidores).hasSize(1);
        assertThat(seguidores.get(0).getNombreSeguidor()).isEqualTo("Ana Torres");
    }
}
