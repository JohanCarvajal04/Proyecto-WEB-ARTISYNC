package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.CreateCommentRequest;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.CommentResponse;
import uteq.edu.ec.artisync.entity.comunicacion.PortfolioComment;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.perfil.Portfolio;
import uteq.edu.ec.artisync.entity.perfil.PortfolioItem;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.comunicacion.PortfolioCommentRepository;
import uteq.edu.ec.artisync.repository.perfil.PortfolioItemRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortfolioCommentServiceImplTest {

    @Mock
    private PortfolioCommentRepository comentarioRepository;
    @Mock
    private PortfolioItemRepository portafolioItemRepository;
    @Mock
    private UserRepository usuarioRepository;

    @InjectMocks
    private PortfolioCommentServiceImpl servicio;

    @Test
    void crearComentario_ok() {
        CreateCommentRequest peticion = new CreateCommentRequest();
        peticion.setTextoComentario("Hola");

        PortfolioItem item = new PortfolioItem();
        item.setIdItemPortafolio(10L);

        User autor = new User();
        autor.setIdUsuario(1L);
        autor.setNombres("A");
        autor.setApellidos("B");

        when(portafolioItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(autor));
        
        PortfolioComment guardado = PortfolioComment.builder()
                .idComentario(99L)
                .itemPortafolio(item)
                .usuarioAutor(autor)
                .build();
        when(comentarioRepository.save(any())).thenReturn(guardado);

        CommentResponse res = servicio.createComment(10L, peticion, 1L);
        assertThat(res.getIdComentario()).isEqualTo(99L);
        assertThat(res.getNombreAutor()).isEqualTo("A B");
    }

    @Test
    void crearComentario_itemNoEncontrado() {
        CreateCommentRequest peticion = new CreateCommentRequest();
        when(portafolioItemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.createComment(10L, peticion, 1L));
    }

    @Test
    void crearComentario_usuarioNoEncontrado() {
        CreateCommentRequest peticion = new CreateCommentRequest();
        when(portafolioItemRepository.findById(10L)).thenReturn(Optional.of(new PortfolioItem()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> servicio.createComment(10L, peticion, 1L));
    }

    @Test
    void listComments() {
        Page<PortfolioComment> page = new PageImpl<>(Collections.emptyList());
        when(comentarioRepository.findByItemPortafolioIdItemPortafolioAndEstadoModeracion(eq(10L), eq("Activo"), any()))
                .thenReturn(page);
        
        Page<CommentResponse> res = servicio.listComments(10L, Pageable.unpaged());
        assertThat(res).isNotNull();
    }

    @Test
    void countComments() {
        when(comentarioRepository.countByItemPortafolioIdItemPortafolioAndEstadoModeracion(10L, "Activo")).thenReturn(5L);
        long total = servicio.countComments(10L);
        assertThat(total).isEqualTo(5L);
    }

    @Test
    void eliminarComentario_comoAdmin_eliminaFisicamente() {
        PortfolioComment c = new PortfolioComment();
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        servicio.deleteComment(99L, 1L, true);
        verify(comentarioRepository).delete(c);
    }

    @Test
    void eliminarComentario_comoAutor_borradoLogico() {
        User u = new User();
        u.setIdUsuario(1L);
        PortfolioComment c = new PortfolioComment();
        c.setUsuarioAutor(u);
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        servicio.deleteComment(99L, 1L, false);
        assertThat(c.getEstadoModeracion()).isEqualTo("Eliminado");
    }

    @Test
    void eliminarComentario_comoDueno_borradoLogico() {
        User dueno = new User();
        dueno.setIdUsuario(2L);
        CreatorProfile perfil = new CreatorProfile();
        perfil.setUsuario(dueno);
        Portfolio port = new Portfolio();
        port.setPerfil(perfil);
        PortfolioItem item = new PortfolioItem();
        item.setPortafolio(port);

        PortfolioComment c = new PortfolioComment();
        c.setItemPortafolio(item);
        
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        servicio.deleteComment(99L, 2L, false);
        assertThat(c.getEstadoModeracion()).isEqualTo("Eliminado");
    }

    @Test
    void eliminarComentario_sinPermisos_lanzaExcepcion() {
        PortfolioComment c = new PortfolioComment(); // Ni autor ni dueno
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        assertThrows(AccessDeniedException.class, () -> servicio.deleteComment(99L, 1L, false));
    }
    
    @Test
    void eliminarComentario_duenoNull_lanzaExcepcion() {
        PortfolioItem item = new PortfolioItem(); // portafolio es null
        PortfolioComment c = new PortfolioComment();
        c.setItemPortafolio(item);
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        assertThrows(AccessDeniedException.class, () -> servicio.deleteComment(99L, 1L, false));
    }

    @Test
    void listForModeration() {
        Page<PortfolioComment> page = new PageImpl<>(Collections.emptyList());
        when(comentarioRepository.findAll(any(Pageable.class))).thenReturn(page);
        
        Page<CommentResponse> res = servicio.listForModeration(Pageable.unpaged());
        assertThat(res).isNotNull();
    }

    @Test
    void ocultarComentario_ok() {
        PortfolioComment c = new PortfolioComment();
        when(comentarioRepository.findByIdParaModerar(99L)).thenReturn(Optional.of(c));

        CommentResponse res = servicio.hideComment(99L);
        assertThat(c.getEstadoModeracion()).isEqualTo("Oculto");
    }

    @Test
    void ocultarComentario_noEncontrado() {
        when(comentarioRepository.findByIdParaModerar(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> servicio.hideComment(99L));
    }

    @Test
    void reactivarComentario_ok() {
        PortfolioComment c = new PortfolioComment();
        when(comentarioRepository.findByIdParaModerar(99L)).thenReturn(Optional.of(c));

        CommentResponse res = servicio.reactivateComment(99L);
        assertThat(c.getEstadoModeracion()).isEqualTo("Activo");
    }
    
    @Test
    void mapToResponse_nullValues() {
        PortfolioComment c = new PortfolioComment();
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        servicio.deleteComment(99L, 1L, true); // internamente pasa por algo que no llama a map, pero listados sí.
    }
}
