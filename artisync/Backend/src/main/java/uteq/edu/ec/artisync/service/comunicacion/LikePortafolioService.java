package uteq.edu.ec.artisync.service.comunicacion;

import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoLike;

/**
 * Servicio de "me gusta" sobre ítems de portafolio.
 * Cualquier usuario autenticado puede dar o quitar like; el conteo y si el
 * visitante ya dio like son datos públicos (el segundo, null-safe si no hay
 * sesión).
 */
public interface LikePortafolioService {

    /** Da like a un ítem de portafolio. Falla si el usuario ya le había dado like. */
    RespuestaEstadoLike darLike(Long idItemPortafolio, Long idUsuario);

    /** Quita el like. Falla si el usuario no le había dado like. */
    RespuestaEstadoLike quitarLike(Long idItemPortafolio, Long idUsuario);

    /**
     * Estado de likes de un ítem: total y si `idUsuario` (puede ser null si no
     * hay sesión) ya le dio like.
     */
    RespuestaEstadoLike obtenerEstado(Long idItemPortafolio, Long idUsuario);
}
