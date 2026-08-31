package uteq.edu.ec.artisync.service.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearComentario;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaComentario;

/**
 * Servicio de comentarios sobre ítems de portafolio.
 * Cualquier usuario autenticado puede comentar una obra publicada;
 * el autor del comentario, el dueño del portafolio o un ADMIN pueden eliminarlo.
 */
public interface ComentarioPortafolioService {

    /** Publica un comentario en un ítem de portafolio. */
    RespuestaComentario crearComentario(Long idItemPortafolio, PeticionCrearComentario peticion, Long idUsuarioAutor);

    /** Lista los comentarios activos de un ítem de portafolio (público, paginado). */
    Page<RespuestaComentario> listarComentarios(Long idItemPortafolio, Pageable pageable);

    /** Número de comentarios activos de un ítem de portafolio. */
    long contarComentarios(Long idItemPortafolio);

    /**
     * Elimina un comentario. Solo puede hacerlo el autor, el dueño del
     * portafolio comentado, o un ADMIN.
     */
    void eliminarComentario(Long idComentario, Long idUsuarioSolicitante, boolean esAdmin);

    /** Lista todos los comentarios del sistema para moderación (solo ADMIN). */
    Page<RespuestaComentario> listarParaModeracion(Pageable pageable);

    /** Oculta un comentario sin eliminarlo (moderación, solo ADMIN). */
    RespuestaComentario ocultarComentario(Long idComentario);

    /** Reactiva un comentario previamente oculto (moderación, solo ADMIN). */
    RespuestaComentario reactivarComentario(Long idComentario);
}
