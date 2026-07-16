package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearTicketRevision;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaTicketRevision;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.TicketRevision;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.pedido.MotivoRechazoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.pedido.TicketRevisionRepository;
import uteq.edu.ec.artisync.service.pedido.ITicketRevisionServicio;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketRevisionServicioImpl implements ITicketRevisionServicio {

    private final TicketRevisionRepository ticketRevisionRepository;
    private final PedidoRepository pedidoRepository;
    private final MotivoRechazoRepository motivoRechazoRepository;
    private final ContratoRepository contratoRepository;

    @Override
    @Transactional
    public RespuestaTicketRevision crearTicketRevision(Long idPedido, Long idCliente,
                                                        PeticionCrearTicketRevision peticion) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));

        // Verificar que el usuario es el cliente del pedido
        if (!pedido.getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new ExcepcionReglaNegocio("Solo el cliente del pedido puede crear tickets de revision");
        }

        var motivo = motivoRechazoRepository.findById(peticion.getIdMotivo())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Motivo de rechazo no encontrado"));

        TicketRevision ticket = TicketRevision.builder()
                .pedido(pedido)
                .motivo(motivo)
                .descripcionCliente(peticion.getDescripcionCliente())
                .estadoTicket("Abierto")
                .build();

        // Verificar si supera el límite de revisiones del contrato
        var contratoOpt = contratoRepository.findByPedidoIdPedido(idPedido);
        if (contratoOpt.isPresent()) {
            long revisionesActuales = ticketRevisionRepository.countByPedidoIdPedido(idPedido);
            int limiteRevisiones = contratoOpt.get().getLimiteRevisiones();

            if (revisionesActuales >= limiteRevisiones && limiteRevisiones > 0) {
                // Supera el límite → marcar costo adicional
                var cargoExtra = pedido.getServicio().getCargoRevisionAdicional();
                ticket.setCostoAdicionalGenerado(cargoExtra);
                log.info("Ticket de revision para pedido {} supera el limite ({}/{}). Cargo adicional: {}",
                        idPedido, revisionesActuales, limiteRevisiones, cargoExtra);
            }
        }

        ticket = ticketRevisionRepository.save(ticket);
        log.info("Ticket de revision {} creado para pedido {}", ticket.getIdTicket(), idPedido);

        return mapToRespuesta(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaTicketRevision> listarTicketsPorPedido(Long idPedido) {
        if (!pedidoRepository.existsById(idPedido)) {
            throw new ExcepcionRecursoNoEncontrado("Pedido no encontrado con ID: " + idPedido);
        }

        return ticketRevisionRepository.findByPedidoIdPedidoOrderByIdTicketDesc(idPedido)
                .stream()
                .map(this::mapToRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RespuestaTicketRevision cambiarEstadoTicket(Long idTicket, Long idCreador, String nuevoEstado) {
        TicketRevision ticket = ticketRevisionRepository.findById(idTicket)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Ticket de revision no encontrado"));

        // Verificar que el usuario es el creador del servicio del pedido
        Long idCreadorServicio = ticket.getPedido().getServicio().getPerfil().getUsuario().getIdUsuario();
        if (!idCreadorServicio.equals(idCreador)) {
            throw new ExcepcionReglaNegocio("Solo el creador del servicio puede cambiar el estado del ticket");
        }

        ticket.setEstadoTicket(nuevoEstado);
        ticket = ticketRevisionRepository.save(ticket);

        log.info("Ticket {} cambio a estado '{}'", idTicket, nuevoEstado);
        return mapToRespuesta(ticket);
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private RespuestaTicketRevision mapToRespuesta(TicketRevision ticket) {
        return RespuestaTicketRevision.builder()
                .idTicket(ticket.getIdTicket())
                .idPedido(ticket.getPedido().getIdPedido())
                .descripcionMotivo(ticket.getMotivo().getDescripcionMotivo())
                .descripcionCliente(ticket.getDescripcionCliente())
                .estadoTicket(ticket.getEstadoTicket())
                .costoAdicionalGenerado(ticket.getCostoAdicionalGenerado())
                .build();
    }
}
