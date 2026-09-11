package uteq.edu.ec.artisync.service.social;

import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearResena;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaResena;

import java.util.List;

/**
 * Offering de reseñas de servicios.
 * RF-09: Una reseña por pedido, solo post-entrega de entregable liberado.
 */
public interface ResenaService {

    /**
     * Crea una reseña para un pedido entregado.
     * Valida: es el cliente del pedido, el entregable está liberado, no hay reseña previa.
     *
     * @param idPedido id del pedido reseñado
     * @param peticion calificación y comentario de la reseña
     * @param idCliente id del usuario que reseña, debe ser el cliente del pedido
     * @return la reseña recién creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     * @throws org.springframework.web.server.ResponseStatusException {@code FORBIDDEN} si el solicitante no es el cliente del pedido
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el entregable del pedido no está liberado
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el cliente ya dejó una reseña para este pedido
     */
    RespuestaResena crearResena(Long idPedido, PeticionCrearResena peticion, Long idCliente);

    /**
     * Obtiene la reseña que el cliente dejó para un pedido, si existe.
     * Devuelve null cuando el pedido todavía no tiene reseña.
     *
     * @param idPedido  id del pedido
     * @param idCliente id del cliente propietario de la reseña
     * @return la reseña del pedido, o {@code null} si aún no existe
     */
    RespuestaResena obtenerMiResena(Long idPedido, Long idCliente);

    /**
     * Edita la reseña de un pedido. Solo el cliente que la creó puede modificarla.
     *
     * @param idPedido  id del pedido reseñado
     * @param peticion  nueva calificación y comentario
     * @param idCliente id del usuario que edita, debe ser quien creó la reseña
     * @return la reseña ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no tiene una reseña
     * @throws org.springframework.web.server.ResponseStatusException {@code FORBIDDEN} si el solicitante no es el autor de la reseña
     */
    RespuestaResena actualizarResena(Long idPedido, PeticionCrearResena peticion, Long idCliente);

    /**
     * Elimina la reseña de un pedido. Solo el cliente que la creó puede eliminarla.
     *
     * @param idPedido  id del pedido reseñado
     * @param idCliente id del usuario que elimina, debe ser quien creó la reseña
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no tiene una reseña
     * @throws org.springframework.web.server.ResponseStatusException {@code FORBIDDEN} si el solicitante no es el autor de la reseña
     */
    void eliminarResena(Long idPedido, Long idCliente);

    /**
     * Lista todas las reseñas de los servicios de un creador (público).
     *
     * @param idPerfilCreador id del perfil de creador
     * @return las reseñas recibidas por el creador
     */
    List<RespuestaResena> listarResenasPorCreador(Long idPerfilCreador);

    /**
     * Calcula el promedio de calificaciones del creador.
     * Retorna 0.0 si no tiene reseñas.
     *
     * @param idPerfilCreador id del perfil de creador
     * @return el promedio de calificaciones, o 0.0 si no tiene reseñas
     */
    Double calcularPromedioPorCreador(Long idPerfilCreador);
}
