package uteq.edu.ec.artisync.service.comunicacion;

import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;

import java.util.List;

/**
 * Servicio de seguimiento de creadores.
 * REQ-F-009: cualquier usuario autenticado puede seguir o dejar de seguir a un creador,
 * y el contador de seguidores del perfil público refleja el cambio de inmediato.
 */
public interface SeguidorService {

    /**
     * Registra que un usuario pasa a seguir a un creador.
     * Valida: el perfil existe, el usuario no se sigue a sí mismo y no seguía ya al creador.
     */
    RespuestaSeguidor seguir(Long idPerfilCreador, Long idUsuarioSeguidor);

    /**
     * Elimina la relación de seguimiento.
     * Falla si el usuario no seguía al creador.
     */
    void dejarDeSeguir(Long idPerfilCreador, Long idUsuarioSeguidor);

    /** Número de seguidores del creador (dato público del perfil). */
    long contarSeguidores(Long idPerfilCreador);

    /** Indica si un usuario concreto sigue al creador. */
    boolean sigue(Long idPerfilCreador, Long idUsuarioSeguidor);

    /** Lista los seguidores de un creador. */
    List<RespuestaSeguidor> listarSeguidores(Long idPerfilCreador);
}
