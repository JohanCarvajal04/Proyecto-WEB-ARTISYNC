package uteq.edu.ec.artisync.service.social;

import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearResena;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaResena;

import java.util.List;

/**
 * Servicio de reseñas de servicios.
 * RF-09: Una reseña por pedido, solo post-entrega de entregable liberado.
 */
public interface ResenaService {

    /**
     * Crea una reseña para un pedido entregado.
     * Valida: es el cliente del pedido, el entregable está liberado, no hay reseña previa.
     */
    RespuestaResena crearResena(Long idPedido, PeticionCrearResena peticion, Long idCliente);

    /** Lista todas las reseñas de los servicios de un creador (público). */
    List<RespuestaResena> listarResenasPorCreador(Long idPerfilCreador);

    /**
     * Calcula el promedio de calificaciones del creador.
     * Retorna 0.0 si no tiene reseñas.
     */
    Double calcularPromedioPorCreador(Long idPerfilCreador);
}
