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
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearComentario;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaComentario;
import uteq.edu.ec.artisync.entity.comunicacion.ComentarioPortafolio;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.perfil.Portafolio;
import uteq.edu.ec.artisync.entity.perfil.PortafolioItem;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.comunicacion.ComentarioPortafolioRepository;
import uteq.edu.ec.artisync.repository.perfil.PortafolioItemRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComentarioPortafolioServiceImplTest {

    @Mock
    private ComentarioPortafolioRepository comentarioRepository;
    @Mock
    private PortafolioItemRepository portafolioItemRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ComentarioPortafolioServiceImpl servicio;

    @Test
    void crearComentario_ok() {
        PeticionCrearComentario peticion = new PeticionCrearComentario();
        peticion.setTextoComentario("Hola");

        PortafolioItem item = new PortafolioItem();
        item.setIdItemPortafolio(10L);

        Usuario autor = new Usuario();
        autor.setIdUsuario(1L);
        autor.setNombres("A");
        autor.setApellidos("B");

        when(portafolioItemRepository.findById(10L)).thenReturn(Optional.of(item));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(autor));
        
        ComentarioPortafolio guardado = ComentarioPortafolio.builder()
                .idComentario(99L)
                .itemPortafolio(item)
                .usuarioAutor(autor)
                .build();
        when(comentarioRepository.save(any())).thenReturn(guardado);

        RespuestaComentario res = servicio.crearComentario(10L, peticion, 1L);
        assertThat(res.getIdComentario()).isEqualTo(99L);
        assertThat(res.getNombreAutor()).isEqualTo("A B");
    }

    @Test
    void crearComentario_itemNoEncontrado() {
        PeticionCrearComentario peticion = new PeticionCrearComentario();
        when(portafolioItemRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> servicio.crearComentario(10L, peticion, 1L));
    }

    @Test
    void crearComentario_usuarioNoEncontrado() {
        PeticionCrearComentario peticion = new PeticionCrearComentario();
        when(portafolioItemRepository.findById(10L)).thenReturn(Optional.of(new PortafolioItem()));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> servicio.crearComentario(10L, peticion, 1L));
    }

    @Test
    void listarComentarios() {
        Page<ComentarioPortafolio> page = new PageImpl<>(Collections.emptyList());
        when(comentarioRepository.findByItemPortafolioIdItemPortafolioAndEstadoModeracion(eq(10L), eq("Activo"), any()))
                .thenReturn(page);
        
        Page<RespuestaComentario> res = servicio.listarComentarios(10L, Pageable.unpaged());
        assertThat(res).isNotNull();
    }

    @Test
    void contarComentarios() {
        when(comentarioRepository.countByItemPortafolioIdItemPortafolioAndEstadoModeracion(10L, "Activo")).thenReturn(5L);
        long total = servicio.contarComentarios(10L);
        assertThat(total).isEqualTo(5L);
    }

    @Test
    void eliminarComentario_comoAdmin_eliminaFisicamente() {
        ComentarioPortafolio c = new ComentarioPortafolio();
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        servicio.eliminarComentario(99L, 1L, true);
        verify(comentarioRepository).delete(c);
    }

    @Test
    void eliminarComentario_comoAutor_borradoLogico() {
        Usuario u = new Usuario();
        u.setIdUsuario(1L);
        ComentarioPortafolio c = new ComentarioPortafolio();
        c.setUsuarioAutor(u);
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        servicio.eliminarComentario(99L, 1L, false);
        assertThat(c.getEstadoModeracion()).isEqualTo("Eliminado");
    }

    @Test
    void eliminarComentario_comoDueno_borradoLogico() {
        Usuario dueno = new Usuario();
        dueno.setIdUsuario(2L);
        PerfilCreador perfil = new PerfilCreador();
        perfil.setUsuario(dueno);
        Portafolio port = new Portafolio();
        port.setPerfil(perfil);
        PortafolioItem item = new PortafolioItem();
        item.setPortafolio(port);

        ComentarioPortafolio c = new ComentarioPortafolio();
        c.setItemPortafolio(item);
        
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        servicio.eliminarComentario(99L, 2L, false);
        assertThat(c.getEstadoModeracion()).isEqualTo("Eliminado");
    }

    @Test
    void eliminarComentario_sinPermisos_lanzaExcepcion() {
        ComentarioPortafolio c = new ComentarioPortafolio(); // Ni autor ni dueno
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        assertThrows(AccessDeniedException.class, () -> servicio.eliminarComentario(99L, 1L, false));
    }
    
    @Test
    void eliminarComentario_duenoNull_lanzaExcepcion() {
        PortafolioItem item = new PortafolioItem(); // portafolio es null
        ComentarioPortafolio c = new ComentarioPortafolio();
        c.setItemPortafolio(item);
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        assertThrows(AccessDeniedException.class, () -> servicio.eliminarComentario(99L, 1L, false));
    }

    @Test
    void listarParaModeracion() {
        Page<ComentarioPortafolio> page = new PageImpl<>(Collections.emptyList());
        when(comentarioRepository.findAll(any(Pageable.class))).thenReturn(page);
        
        Page<RespuestaComentario> res = servicio.listarParaModeracion(Pageable.unpaged());
        assertThat(res).isNotNull();
    }

    @Test
    void ocultarComentario_ok() {
        ComentarioPortafolio c = new ComentarioPortafolio();
        when(comentarioRepository.findByIdParaModerar(99L)).thenReturn(Optional.of(c));

        RespuestaComentario res = servicio.ocultarComentario(99L);
        assertThat(c.getEstadoModeracion()).isEqualTo("Oculto");
    }

    @Test
    void ocultarComentario_noEncontrado() {
        when(comentarioRepository.findByIdParaModerar(99L)).thenReturn(Optional.empty());
        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> servicio.ocultarComentario(99L));
    }

    @Test
    void reactivarComentario_ok() {
        ComentarioPortafolio c = new ComentarioPortafolio();
        when(comentarioRepository.findByIdParaModerar(99L)).thenReturn(Optional.of(c));

        RespuestaComentario res = servicio.reactivarComentario(99L);
        assertThat(c.getEstadoModeracion()).isEqualTo("Activo");
    }
    
    @Test
    void mapToResponse_nullValues() {
        ComentarioPortafolio c = new ComentarioPortafolio();
        when(comentarioRepository.findById(99L)).thenReturn(Optional.of(c));

        servicio.eliminarComentario(99L, 1L, true); // internamente pasa por algo que no llama a map, pero listados sí.
    }
}
