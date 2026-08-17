package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearComentario;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaComentario;
import uteq.edu.ec.artisync.entity.comunicacion.ComentarioPortafolio;
import uteq.edu.ec.artisync.entity.perfil.PortafolioItem;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.comunicacion.ComentarioPortafolioRepository;
import uteq.edu.ec.artisync.repository.perfil.PortafolioItemRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.ComentarioService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComentarioServiceImpl implements ComentarioService {

    private final ComentarioPortafolioRepository comentarioRepository;
    private final PortafolioItemRepository portafolioItemRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public RespuestaComentario agregarComentario(Long idUsuario, Long idItemPortafolio, PeticionCrearComentario peticion) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        PortafolioItem item = portafolioItemRepository.findById(idItemPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException("Item de portafolio no encontrado"));

        ComentarioPortafolio comentario = ComentarioPortafolio.builder()
                .usuarioAutor(usuario)
                .itemPortafolio(item)
                .textoComentario(peticion.getTextoComentario())
                .estadoModeracion("Activo")
                .build();

        comentario = comentarioRepository.save(comentario);
        log.info("Comentario {} creado por el usuario {} en el item {}", comentario.getIdComentario(), idUsuario, idItemPortafolio);
        return mapearRespuesta(comentario);
    }

    @Override
    @Transactional
    public RespuestaMensaje eliminarComentario(Long idUsuario, Long idComentario) {
        ComentarioPortafolio comentario = comentarioRepository.findById(idComentario)
                .orElseThrow(() -> new ResourceNotFoundException("Comentario no encontrado"));

        // Verificar permisos (el autor o el dueño del portafolio)
        Long idAutor = comentario.getUsuarioAutor().getIdUsuario();
        Long idDuenoPortafolio = comentario.getItemPortafolio().getPortafolio().getPerfil().getUsuario().getIdUsuario();

        if (!idAutor.equals(idUsuario) && !idDuenoPortafolio.equals(idUsuario)) {
            // Check if user is admin
            Usuario usuario = usuarioRepository.findById(idUsuario).orElseThrow();
            boolean isAdmin = usuario.getRol().getNombreRol().equals("ADMINISTRADOR");
            if (!isAdmin) {
                throw new IllegalArgumentException("No tienes permisos para eliminar este comentario");
            }
        }

        comentario.setEstadoModeracion("Oculto");
        comentarioRepository.save(comentario);
        log.info("Comentario {} ocultado lógicamente por el usuario {}", idComentario, idUsuario);
        return new RespuestaMensaje("Comentario eliminado exitosamente");
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaComentario> listarComentariosPorItem(Long idItemPortafolio, Pageable pageable) {
        Page<ComentarioPortafolio> comentarios = comentarioRepository
                .findByItemPortafolioIdItemPortafolioAndEstadoModeracion(idItemPortafolio, "Activo", pageable);
        return comentarios.map(this::mapearRespuesta);
    }

    private RespuestaComentario mapearRespuesta(ComentarioPortafolio comentario) {
        return RespuestaComentario.builder()
                .idComentario(comentario.getIdComentario())
                .idItemPortafolio(comentario.getItemPortafolio().getIdItemPortafolio())
                .idUsuarioAutor(comentario.getUsuarioAutor().getIdUsuario())
                .nombreAutor(comentario.getUsuarioAutor().getNombreUsuario())
                .textoComentario(comentario.getTextoComentario())
                .estadoModeracion(comentario.getEstadoModeracion())
                .fechaPublicacion(comentario.getFechaPublicacion())
                .build();
    }
}
