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

    /**
     * Publica un comentario en un ítem de portafolio.
     *
     * @param idItemPortafolio id del ítem comentado
     * @param peticion         texto del comentario
     * @param idUsuarioAutor   id del usuario autor del comentario
     * @return el comentario recién creado
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el ítem de portafolio o el usuario no existen
     */
    RespuestaComentario crearComentario(Long idItemPortafolio, PeticionCrearComentario peticion, Long idUsuarioAutor);

    /**
     * Lista los comentarios activos de un ítem de portafolio (público, paginado).
     *
     * @param idItemPortafolio id del ítem de portafolio
     * @param pageable         configuración de paginación y orden
     * @return la página de comentarios activos
     */
    Page<RespuestaComentario> listarComentarios(Long idItemPortafolio, Pageable pageable);

    /**
     * Número de comentarios activos de un ítem de portafolio.
     *
     * @param idItemPortafolio id del ítem de portafolio
     * @return la cantidad de comentarios activos
     */
    long contarComentarios(Long idItemPortafolio);

    /**
     * Elimina un comentario. Solo puede hacerlo el autor, el dueño del
     * portafolio comentado, o un ADMIN.
     *
     * @param idComentario         id del comentario a eliminar
     * @param idUsuarioSolicitante id del usuario que solicita la eliminación
     * @param esAdmin              si el solicitante tiene rol de administrador
     * @throws org.springframework.security.access.AccessDeniedException si el solicitante no es el autor, el dueño del portafolio, ni administrador
     */
    void eliminarComentario(Long idComentario, Long idUsuarioSolicitante, boolean esAdmin);

    /**
     * Lista todos los comentarios del sistema para moderación (solo ADMIN).
     *
     * @param pageable configuración de paginación y orden
     * @return la página de todos los comentarios, activos u ocultos
     */
    Page<RespuestaComentario> listarParaModeracion(Pageable pageable);

    /**
     * Oculta un comentario sin eliminarlo (moderación, solo ADMIN).
     *
     * @param idComentario id del comentario a ocultar
     * @return el comentario ya marcado como oculto
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el comentario no existe
     */
    RespuestaComentario ocultarComentario(Long idComentario);

    /**
     * Reactiva un comentario previamente oculto (moderación, solo ADMIN).
     *
     * @param idComentario id del comentario a reactivar
     * @return el comentario ya marcado como activo
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el comentario no existe
     */
    RespuestaComentario reactivarComentario(Long idComentario);
}
