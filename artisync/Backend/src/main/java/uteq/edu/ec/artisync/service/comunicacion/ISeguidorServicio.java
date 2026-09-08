package uteq.edu.ec.artisync.service.comunicacion;

import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaCreadorSeguidoNovedad;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoSeguimiento;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;

import java.util.List;

public interface ISeguidorServicio {

    /**
     * Registra al usuario indicado como seguidor de un creador.
     *
     * @param idUsuarioSeguidor id del usuario que sigue
     * @param idPerfilCreador   id del perfil de creador a seguir
     * @return el estado de seguimiento resultante
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el perfil de creador no existe
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el usuario intenta seguirse a sí mismo como creador
     */
    RespuestaEstadoSeguimiento seguirCreador(Long idUsuarioSeguidor, Long idPerfilCreador);

    /**
     * Quita el seguimiento del usuario indicado sobre un creador.
     *
     * @param idUsuarioSeguidor id del usuario que deja de seguir
     * @param idPerfilCreador   id del perfil de creador dejado de seguir
     * @return el estado de seguimiento resultante
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el perfil de creador no existe
     */
    RespuestaEstadoSeguimiento dejarDeSeguirCreador(Long idUsuarioSeguidor, Long idPerfilCreador);

    /**
     * Consulta si un usuario sigue a un creador.
     *
     * @param idUsuarioConsulta id del usuario que consulta, o {@code null} si no hay sesión
     * @param idPerfilCreador   id del perfil de creador consultado
     * @return el estado de seguimiento entre ambos
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el perfil de creador no existe
     */
    RespuestaEstadoSeguimiento obtenerEstadoSeguimiento(Long idUsuarioConsulta, Long idPerfilCreador);

    /**
     * Lista los seguidores de un creador.
     *
     * @param idPerfilCreador id del perfil de creador
     * @return los seguidores del creador
     */
    List<RespuestaSeguidor> listarSeguidores(Long idPerfilCreador);

    /**
     * Lista las novedades de los creadores que sigue un usuario.
     *
     * @param idUsuarioSeguidor id del usuario seguidor
     * @return las novedades de los creadores seguidos
     */
    List<RespuestaCreadorSeguidoNovedad> listarCreadoresSeguidosNovedades(Long idUsuarioSeguidor);

    /**
     * Actualiza la imagen de portada y el título profesional del perfil de creador propio.
     *
     * @param idUsuario         id de usuario del creador autenticado
     * @param urlPortada        nueva URL de la imagen de portada
     * @param tituloProfesional nuevo título profesional
     * @return {@code true} si la actualización se aplicó
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el usuario no tiene un perfil de creador asociado
     */
    boolean actualizarPortadaYTitulo(Long idUsuario, String urlPortada, String tituloProfesional);
}
