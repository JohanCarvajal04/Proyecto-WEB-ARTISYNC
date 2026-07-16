package uteq.edu.ec.artisync.service.pedido;

import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearTicketRevision;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaTicketRevision;

import java.util.List;

public interface ITicketRevisionServicio {

    RespuestaTicketRevision crearTicketRevision(Long idPedido, Long idCliente, PeticionCrearTicketRevision peticion);

    List<RespuestaTicketRevision> listarTicketsPorPedido(Long idPedido);

    RespuestaTicketRevision cambiarEstadoTicket(Long idTicket, Long idCreador, String nuevoEstado);
}
