package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoLike;
import uteq.edu.ec.artisync.entity.comunicacion.LikePortafolio;
import uteq.edu.ec.artisync.entity.perfil.PortfolioItem;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.comunicacion.LikePortafolioRepository;
import uteq.edu.ec.artisync.repository.perfil.PortfolioItemRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LikePortafolioServiceImplTest {

    @Mock
    private LikePortafolioRepository likeRepository;

    @Mock
    private PortfolioItemRepository portafolioItemRepository;

    @Mock
    private UserRepository usuarioRepository;

    @InjectMocks
    private LikePortafolioServiceImpl likePortafolioServicio;

    private PortfolioItem item;
    private User usuario;

    private void prepararItemYUsuario() {
        item = PortfolioItem.builder().idItemPortafolio(1L).build();
        usuario = User.builder().idUsuario(20L).build();
    }

    @Test
    @DisplayName("darLike — exito cuando el usuario no le habia dado like antes")
    void darLike_exito() {
        prepararItemYUsuario();
        given(portafolioItemRepository.findById(1L)).willReturn(Optional.of(item));
        given(likeRepository.existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(1L, 20L)).willReturn(false);
        given(usuarioRepository.findById(20L)).willReturn(Optional.of(usuario));
        given(likeRepository.countByItemPortafolioIdItemPortafolio(1L)).willReturn(5L);

        RespuestaEstadoLike respuesta = likePortafolioServicio.darLike(1L, 20L);

        assertThat(respuesta.isMeGusta()).isTrue();
        assertThat(respuesta.getTotalLikes()).isEqualTo(5L);
        verify(likeRepository).save(any(LikePortafolio.class));
    }

    @Test
    @DisplayName("darLike — lanza excepcion si el item de portafolio no existe")
    void darLike_itemNoExiste_lanzaExcepcion() {
        given(portafolioItemRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> likePortafolioServicio.darLike(99L, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("darLike — lanza excepcion si ya le habia dado like")
    void darLike_yaLeDioLike_lanzaExcepcion() {
        prepararItemYUsuario();
        given(portafolioItemRepository.findById(1L)).willReturn(Optional.of(item));
        given(likeRepository.existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(1L, 20L)).willReturn(true);

        assertThatThrownBy(() -> likePortafolioServicio.darLike(1L, 20L))
                .isInstanceOf(DuplicateResourceException.class);
        verify(likeRepository, never()).save(any(LikePortafolio.class));
    }

    @Test
    @DisplayName("darLike — lanza excepcion si el usuario no existe")
    void darLike_usuarioNoExiste_lanzaExcepcion() {
        prepararItemYUsuario();
        given(portafolioItemRepository.findById(1L)).willReturn(Optional.of(item));
        given(likeRepository.existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(1L, 20L)).willReturn(false);
        given(usuarioRepository.findById(20L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> likePortafolioServicio.darLike(1L, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("quitarLike — exito")
    void quitarLike_exito() {
        LikePortafolio like = LikePortafolio.builder().idLike(7L).build();
        given(likeRepository.findByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(1L, 20L))
                .willReturn(Optional.of(like));
        given(likeRepository.countByItemPortafolioIdItemPortafolio(1L)).willReturn(4L);

        RespuestaEstadoLike respuesta = likePortafolioServicio.quitarLike(1L, 20L);

        assertThat(respuesta.isMeGusta()).isFalse();
        assertThat(respuesta.getTotalLikes()).isEqualTo(4L);
        verify(likeRepository).delete(like);
    }

    @Test
    @DisplayName("quitarLike — lanza excepcion si no le habia dado like antes")
    void quitarLike_noExiste_lanzaExcepcion() {
        given(likeRepository.findByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(1L, 20L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> likePortafolioServicio.quitarLike(1L, 20L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("obtenerEstado — usuario anonimo nunca tiene like")
    void obtenerEstado_usuarioAnonimo_meGustaFalso() {
        given(likeRepository.countByItemPortafolioIdItemPortafolio(1L)).willReturn(10L);

        RespuestaEstadoLike respuesta = likePortafolioServicio.obtenerEstado(1L, null);

        assertThat(respuesta.isMeGusta()).isFalse();
        assertThat(respuesta.getTotalLikes()).isEqualTo(10L);
    }

    @Test
    @DisplayName("obtenerEstado — usuario autenticado que ya le dio like")
    void obtenerEstado_usuarioConLike_meGustaVerdadero() {
        given(likeRepository.existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(1L, 20L)).willReturn(true);
        given(likeRepository.countByItemPortafolioIdItemPortafolio(1L)).willReturn(10L);

        RespuestaEstadoLike respuesta = likePortafolioServicio.obtenerEstado(1L, 20L);

        assertThat(respuesta.isMeGusta()).isTrue();
    }

    @Test
    @DisplayName("obtenerEstado — usuario autenticado que no le ha dado like")
    void obtenerEstado_usuarioSinLike_meGustaFalso() {
        given(likeRepository.existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(1L, 20L)).willReturn(false);
        given(likeRepository.countByItemPortafolioIdItemPortafolio(1L)).willReturn(10L);

        RespuestaEstadoLike respuesta = likePortafolioServicio.obtenerEstado(1L, 20L);

        assertThat(respuesta.isMeGusta()).isFalse();
    }
}
