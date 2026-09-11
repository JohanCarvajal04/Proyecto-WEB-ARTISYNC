package uteq.edu.ec.artisync.service.comunicacion;

import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoLike;

/**
 * Servicio de "me gusta" sobre ítems de portafolio.
 * Cualquier usuario autenticado puede dar o quitar like; el conteo y si el
 * visitante ya dio like son datos públicos (el segundo, null-safe si no hay
 * sesión).
 */
public interface LikePortafolioService {

    /**
     * Da like a un ítem de portafolio. Falla si el usuario ya le había dado like.
     *
     * @param idItemPortafolio id del ítem de portafolio
     * @param idUsuario        id del usuario que da like
     * @return el estado de likes resultante
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el usuario ya le había dado like
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el ítem de portafolio o el usuario no existen
     */
    RespuestaEstadoLike darLike(Long idItemPortafolio, Long idUsuario);

    /**
     * Quita el like. Falla si el usuario no le había dado like.
     *
     * @param idItemPortafolio id del ítem de portafolio
     * @param idUsuario        id del usuario que quita el like
     * @return el estado de likes resultante
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no le había dado like a ese ítem
     */
    RespuestaEstadoLike quitarLike(Long idItemPortafolio, Long idUsuario);

    /**
     * Estado de likes de un ítem: total y si `idUsuario` (puede ser null si no
     * hay sesión) ya le dio like.
     *
     * @param idItemPortafolio id del ítem de portafolio
     * @param idUsuario        id del usuario consultante, o {@code null} si no hay sesión
     * @return el estado de likes del ítem
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el ítem de portafolio no existe
     */
    RespuestaEstadoLike obtenerEstado(Long idItemPortafolio, Long idUsuario);
}
