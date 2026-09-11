package uteq.edu.ec.artisync.service.pedido;

import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearFlujoTrabajo;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionEtapaConfig;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionSwapEtapas;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaFlujoTrabajo;

import java.util.List;

public interface IFlujoTrabajoServicio {

    /**
     * Crea un flujo de trabajo para un creador, opcionalmente con sus etapas iniciales.
     *
     * @param idUsuario id del creador dueño del flujo
     * @param peticion  nombre, descripción y etapas iniciales del flujo
     * @return el flujo recién creado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no existe
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el creador ya tiene un flujo con el mismo nombre
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si las etapas iniciales tienen nombres o números de orden repetidos
     */
    RespuestaFlujoTrabajo crearFlujoTrabajo(Long idUsuario, PeticionCrearFlujoTrabajo peticion);

    /** puedeVerTodos: el llamador tiene FLUJO_MODERAR (o es ADMIN) — ve los flujos de todos los creadores, no solo los suyos.
     *
     * @param idUsuario     id del usuario que consulta
     * @param puedeVerTodos si puede ver los flujos de todos los creadores
     * @return los flujos de trabajo visibles para el solicitante
     */
    List<RespuestaFlujoTrabajo> listarFlujosTrabajo(Long idUsuario, boolean puedeVerTodos);

    /**
     * Obtiene el detalle de un flujo de trabajo, con sus etapas configuradas.
     *
     * @param idFlujo       id del flujo
     * @param idUsuario     id del usuario que consulta
     * @param puedeVerTodos si puede acceder a flujos de cualquier creador
     * @return el detalle del flujo de trabajo
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el flujo no existe o no es accesible para el solicitante
     */
    RespuestaFlujoTrabajo obtenerFlujoPorId(Long idFlujo, Long idUsuario, boolean puedeVerTodos);

    /**
     * Actualiza el nombre y descripción de un flujo de trabajo.
     *
     * @param idFlujo       id del flujo a actualizar
     * @param idUsuario     id del usuario que solicita
     * @param puedeVerTodos si puede acceder a flujos de cualquier creador
     * @param peticion      nuevo nombre y descripción del flujo
     * @return el flujo ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el flujo no existe o no es accesible para el solicitante
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el dueño del flujo ya tiene otro flujo con el nuevo nombre
     */
    RespuestaFlujoTrabajo actualizarFlujoTrabajo(Long idFlujo, Long idUsuario, boolean puedeVerTodos, PeticionCrearFlujoTrabajo peticion);

    /**
     * Agrega una nueva etapa a un flujo de trabajo existente.
     *
     * @param idFlujo       id del flujo
     * @param idUsuario     id del usuario que solicita
     * @param puedeVerTodos si puede acceder a flujos de cualquier creador
     * @param peticion      nombre, orden y configuración de la nueva etapa
     * @return el flujo con la etapa ya agregada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el flujo no existe o no es accesible para el solicitante
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si la etapa ya existe en este flujo
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya hay una etapa con el mismo número de orden en el flujo
     */
    RespuestaFlujoTrabajo agregarEtapa(Long idFlujo, Long idUsuario, boolean puedeVerTodos, PeticionEtapaConfig peticion);

    /**
     * Actualiza la configuración (orden, si es final, si requiere entregable) de una etapa de un flujo.
     *
     * @param idFlujo       id del flujo
     * @param idFlujoEtapa  id de la configuración de etapa a actualizar
     * @param idUsuario     id del usuario que solicita
     * @param puedeVerTodos si puede acceder a flujos de cualquier creador
     * @param peticion      nueva configuración de la etapa
     * @return el flujo con la etapa ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el flujo o la configuración de etapa no existen, o el flujo no es accesible
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la etapa no pertenece al flujo indicado o el nuevo orden ya está ocupado por otra etapa
     */
    RespuestaFlujoTrabajo actualizarEtapa(Long idFlujo, Long idFlujoEtapa, Long idUsuario, boolean puedeVerTodos, PeticionEtapaConfig peticion);

    /** Swap atómico de numeroOrden entre dos etapas — lo usa "mover etapa arriba/abajo".
     *
     * @param idFlujo       id del flujo
     * @param idUsuario     id del usuario que solicita
     * @param puedeVerTodos si puede acceder a flujos de cualquier creador
     * @param peticion      ids de las dos configuraciones de etapa a intercambiar
     * @return el flujo con el orden de las etapas ya intercambiado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el flujo o alguna de las configuraciones de etapa no existen, o el flujo no es accesible
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si se intenta intercambiar una etapa consigo misma o alguna etapa no pertenece al flujo indicado
     */
    RespuestaFlujoTrabajo intercambiarOrdenEtapas(Long idFlujo, Long idUsuario, boolean puedeVerTodos, PeticionSwapEtapas peticion);

    /**
     * Elimina una etapa de un flujo, siempre que ningún pedido esté actualmente detenido en ella.
     *
     * @param idFlujo       id del flujo
     * @param idFlujoEtapa  id de la configuración de etapa a eliminar
     * @param idUsuario     id del usuario que solicita
     * @param puedeVerTodos si puede acceder a flujos de cualquier creador
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la configuración de etapa no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la etapa no pertenece al flujo o el solicitante no tiene permisos, o hay pedidos detenidos en esa etapa
     */
    void eliminarEtapa(Long idFlujo, Long idFlujoEtapa, Long idUsuario, boolean puedeVerTodos);
}
