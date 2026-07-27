package uteq.edu.ec.artisync.service.social;

import uteq.edu.ec.artisync.dto.peticion.social.PeticionActualizarSorteo;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearSorteo;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaGanador;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaParticipante;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaSorteo;

import java.util.List;

/**
 * Servicio de sorteos configurables.
 * RF-23: CRUD de sorteos, inscripción con validaciones y selección automática de ganadores.
 */
public interface SorteoService {

    // --- CRUD de sorteos (CREADOR) ---

    /** Crea un nuevo sorteo. El creador se resuelve desde el idUsuario del JWT. */
    RespuestaSorteo crearSorteo(Long idUsuario, PeticionCrearSorteo peticion);

    /** Obtiene el detalle de un sorteo. Indica si el usuario actual ya participa. */
    RespuestaSorteo obtenerSorteo(Long idSorteo, Long idUsuarioActual);

    /**
     * Actualiza un sorteo. Aplica restricciones de la guía:
     * - cantidadGanadores y fechaCierre NO modificables si ya hay participantes.
     */
    RespuestaSorteo actualizarSorteo(Long idSorteo, Long idUsuario, PeticionActualizarSorteo peticion);

    /** Elimina un sorteo solo si no tiene participantes y pertenece al creador. */
    RespuestaMensaje eliminarSorteo(Long idSorteo, Long idUsuario);

    /** Lista todos los sorteos de un creador (público). */
    List<RespuestaSorteo> listarSorteosPorCreador(Long idPerfilCreador, Long idUsuarioActual);

    /** Lista todos los sorteos activos (público). */
    List<RespuestaSorteo> listarSorteosActivos(Long idUsuarioActual);

    // --- Participación ---

    /** Inscribe al usuario en un sorteo, validando estado, fechas y requisito de seguidor. */
    RespuestaParticipante participar(Long idSorteo, Long idUsuario);

    /** Cancela la inscripción del usuario si el sorteo todavía está activo. */
    RespuestaMensaje cancelarParticipacion(Long idSorteo, Long idUsuario);

    /** Lista todos los participantes de un sorteo. */
    List<RespuestaParticipante> listarParticipantes(Long idSorteo);

    /** Lista los ganadores de un sorteo (solo post-cierre). */
    List<RespuestaGanador> listarGanadores(Long idSorteo);
}
