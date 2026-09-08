package uteq.edu.ec.artisync.service.comunicacion;

import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearBriefingPlantilla;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;

import java.util.List;

/**
 * Servicio de briefing interactivo.
 * REQ-F-016 ampliado: el cuestionario se asigna a un servicio
 * (Servicio.briefingPlantilla) y el Cliente lo responde al crear el pedido
 * (ver PedidoServicioImpl.crearPedido), no con un envío manual posterior del
 * Creador. Las respuestas son inmutables una vez creado el pedido.
 */
public interface BriefingService {

    // --- Gestión de plantillas (CREADOR) ---
    // idUsuario: el del JWT del creador autenticado (CustomUserDetails), no el
    // id_perfil — la implementación resuelve el PerfilCreador correspondiente.

    /**
     * Crea una plantilla de briefing (cuestionario) para el perfil de creador del usuario.
     *
     * @param idUsuario id de usuario del creador autenticado
     * @param peticion  nombre de la plantilla y sus preguntas
     * @return la plantilla recién creada
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el usuario no tiene un perfil de creador configurado
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el cuestionario supera el máximo de preguntas permitido
     */
    RespuestaBriefing crearPlantilla(Long idUsuario, PeticionCrearBriefingPlantilla peticion);

    /**
     * Lista las plantillas de briefing propias del creador.
     *
     * @param idUsuario id de usuario del creador autenticado
     * @return las plantillas del creador
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el usuario no tiene un perfil de creador configurado
     */
    List<RespuestaBriefing> obtenerMisPlantillas(Long idUsuario);

    /**
     * Reemplaza el nombre y las preguntas de una plantilla existente.
     *
     * @param idPlantilla id de la plantilla a editar
     * @param idUsuario   id de usuario del creador autenticado, debe ser el dueño de la plantilla
     * @param peticion    nuevo nombre y preguntas de la plantilla
     * @return la plantilla ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si la plantilla no existe
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el solicitante no es el dueño de la plantilla, o si el cuestionario supera el máximo de preguntas permitido
     */
    RespuestaBriefing editarPlantilla(Long idPlantilla, Long idUsuario, PeticionCrearBriefingPlantilla peticion);

    /**
     * Elimina una plantilla de briefing.
     *
     * @param idPlantilla id de la plantilla a eliminar
     * @param idUsuario   id de usuario del creador autenticado, debe ser el dueño de la plantilla
     * @return mensaje de confirmación
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si la plantilla no existe
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el solicitante no es el dueño de la plantilla
     */
    RespuestaMensaje eliminarPlantilla(Long idPlantilla, Long idUsuario);

    /**
     * Obtiene el briefing respondido de un pedido (solo lectura). Solo el
     * cliente/creador del pedido o un ADMIN pueden consultarlo.
     *
     * @param idPedido             id del pedido
     * @param idUsuarioSolicitante id del usuario que consulta
     * @return el briefing respondido del pedido
     * @throws uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado si el pedido no tiene un briefing respondido
     * @throws org.springframework.security.access.AccessDeniedException si el solicitante no es parte del pedido ni administrador
     */
    RespuestaBriefing obtenerBriefing(Long idPedido, Long idUsuarioSolicitante);
}
