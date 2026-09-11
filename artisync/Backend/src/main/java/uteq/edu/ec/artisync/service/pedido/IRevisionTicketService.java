package uteq.edu.ec.artisync.service.pedido;

import uteq.edu.ec.artisync.dto.peticion.pedido.CreateRevisionTicketRequest;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RevisionTicketResponse;

import java.util.List;

public interface IRevisionTicketService {

    /**
     * Crea un ticket de solicitud de revisión sobre un pedido, con el motivo de rechazo indicado.
     *
     * @param idPedido  id del pedido sobre el que se solicita la revisión
     * @param idCliente id del usuario que solicita, debe ser el cliente del pedido
     * @param peticion  motivo del rechazo y observaciones
     * @return el ticket de revisión recién creado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido o el motivo indicado no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es el cliente del pedido
     */
    RevisionTicketResponse crearTicketRevision(Long idPedido, Long idCliente, CreateRevisionTicketRequest peticion);

    /**
     * Lista los tickets de revisión asociados a un pedido.
     *
     * @param idPedido             id del pedido
     * @param idUsuarioSolicitante id del usuario que consulta
     * @return los tickets de revisión del pedido
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     */
    List<RevisionTicketResponse> listarTicketsPorPedido(Long idPedido, Long idUsuarioSolicitante);

    /**
     * Cambia el estado de un ticket de revisión. Solo puede hacerlo el creador
     * del servicio asociado o un usuario con permiso de soporte/administración.
     *
     * @param idTicket  id del ticket a actualizar
     * @param idCreador id del usuario que solicita el cambio
     * @param nuevoEstado nombre del nuevo estado del ticket
     * @return el ticket con su estado ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el ticket no existe
     * @throws org.springframework.security.access.AccessDeniedException si el solicitante no es el creador del servicio ni tiene permiso de soporte/administración
     */
    RevisionTicketResponse cambiarEstadoTicket(Long idTicket, Long idCreador, String nuevoEstado);
}
