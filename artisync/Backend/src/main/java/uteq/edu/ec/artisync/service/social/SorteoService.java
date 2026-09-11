package uteq.edu.ec.artisync.service.social;

import uteq.edu.ec.artisync.dto.peticion.social.PeticionActualizarSorteo;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearSorteo;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaGanador;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaParticipante;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaSorteo;

import java.util.List;

/**
 * Offering de sorteos configurables.
 * RF-23: CRUD de sorteos, inscripción con validaciones y selección automática de ganadores.
 */
public interface SorteoService {

    // --- CRUD de sorteos (CREADOR) ---

    /**
     * Crea un nuevo sorteo. El creador se resuelve desde el idUsuario del JWT.
     *
     * @param idUsuario id del usuario autenticado, dueño del perfil de creador
     * @param peticion  título, fechas, cantidad de ganadores y premios del sorteo
     * @return el sorteo recién creado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no tiene un perfil de creador activo
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la fecha de cierre no es posterior a la de inicio o la cantidad de premios no coincide con la cantidad de ganadores
     */
    RespuestaSorteo crearSorteo(Long idUsuario, PeticionCrearSorteo peticion);

    /**
     * Obtiene el detalle de un sorteo. Indica si el usuario actual ya participa.
     *
     * @param idSorteo        id del sorteo
     * @param idUsuarioActual id del usuario que consulta, o {@code null} si es anónimo
     * @return el detalle del sorteo
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     */
    RespuestaSorteo obtenerSorteo(Long idSorteo, Long idUsuarioActual);

    /**
     * Actualiza un sorteo. Aplica restricciones de la guía:
     * - cantidadGanadores y fechaCierre NO modificables si ya hay participantes.
     *
     * @param idSorteo  id del sorteo a actualizar
     * @param idUsuario id del usuario autenticado, debe ser el creador del sorteo
     * @param peticion  campos a modificar; los nulos se dejan sin cambios
     * @return el sorteo ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe o el usuario no tiene perfil de creador
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si se intenta modificar cantidad de ganadores, premios o fecha de cierre con participantes ya inscritos, o la nueva fecha de cierre es anterior a la de inicio
     */
    RespuestaSorteo actualizarSorteo(Long idSorteo, Long idUsuario, PeticionActualizarSorteo peticion);

    /**
     * Elimina un sorteo solo si no tiene participantes y pertenece al creador.
     *
     * @param idSorteo  id del sorteo a eliminar
     * @param idUsuario id del usuario autenticado, debe ser el creador del sorteo
     * @return mensaje de confirmación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe o el usuario no tiene perfil de creador
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el sorteo ya tiene participantes inscritos
     */
    RespuestaMensaje eliminarSorteo(Long idSorteo, Long idUsuario);

    /**
     * Lista todos los sorteos de un creador (público).
     *
     * @param idPerfilCreador id del perfil creador
     * @param idUsuarioActual id del usuario que consulta, o {@code null} si es anónimo
     * @return los sorteos del creador
     */
    List<RespuestaSorteo> listarSorteosPorCreador(Long idPerfilCreador, Long idUsuarioActual);

    /**
     * Lista todos los sorteos activos (público).
     *
     * @param idUsuarioActual id del usuario que consulta, o {@code null} si es anónimo
     * @return los sorteos actualmente activos
     */
    List<RespuestaSorteo> listarSorteosActivos(Long idUsuarioActual);

    // --- Participación ---

    /**
     * Inscribe al usuario en un sorteo, validando estado, fechas y requisito de seguidor.
     *
     * @param idSorteo  id del sorteo
     * @param idUsuario id del usuario que se inscribe
     * @return la participación recién creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el usuario ya está inscrito en el sorteo
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el sorteo no está activo, está fuera del rango de fechas o exige seguir al creador y el usuario no lo sigue
     */
    RespuestaParticipante participar(Long idSorteo, Long idUsuario);

    /**
     * Cancela la inscripción del usuario si el sorteo todavía está activo.
     *
     * @param idSorteo  id del sorteo
     * @param idUsuario id del usuario cuya inscripción se cancela
     * @return mensaje de confirmación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe o el usuario no está inscrito
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el sorteo ya finalizó
     */
    RespuestaMensaje cancelarParticipacion(Long idSorteo, Long idUsuario);

    /**
     * Lista todos los participantes de un sorteo.
     *
     * @param idSorteo id del sorteo
     * @return los participantes inscritos en el sorteo
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     */
    List<RespuestaParticipante> listarParticipantes(Long idSorteo);

    /**
     * Lista los ganadores de un sorteo (solo post-cierre).
     *
     * @param idSorteo id del sorteo
     * @return los ganadores del sorteo, con su premio asignado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     */
    List<RespuestaGanador> listarGanadores(Long idSorteo);
}
