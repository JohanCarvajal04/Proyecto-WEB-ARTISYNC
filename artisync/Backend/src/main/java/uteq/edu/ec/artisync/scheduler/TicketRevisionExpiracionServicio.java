package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.TicketRevision;
import uteq.edu.ec.artisync.repository.legal.PagoTicketRevisionRepository;
import uteq.edu.ec.artisync.repository.pedido.TicketRevisionRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

/**
 * Extraído de TicketRevisionExpiracionScheduler para que REQUIRES_NEW funcione
 * de verdad (mismo motivo documentado en SorteoEjecutorServicio).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketRevisionExpiracionServicio {

    private static final String ESTADO_ABIERTO = "Abierto";
    private static final String ESTADO_RECHAZADO = "Rechazado";
    private static final String PAGO_PENDIENTE = "Pendiente";
    private static final String PAGO_EXPIRADO = "Expirado";

    private final TicketRevisionRepository ticketRevisionRepository;
    private final PagoTicketRevisionRepository pagoTicketRevisionRepository;
    private final NotificacionService notificacionService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditable(accion = "TICKET_EXPIRAR", modulo = ModuloAuditoria.PEDIDOS,
            correoActor = "'sistema:scheduler'",
            entidad = "tickets_revision", idEntidad = "#idTicket")
    public void expirarTicket(Long idTicket) {
        // Relectura con lock: si el creador ya resolvió el ticket, o el
        // webhook de PayPal ya confirmó el pago, entre que el scheduler lo
        // leyó y esta transacción arrancó, no hay nada que hacer.
        TicketRevision ticket = ticketRevisionRepository.findByIdParaActualizar(idTicket).orElse(null);
        if (ticket == null || !ESTADO_ABIERTO.equals(ticket.getEstadoTicket())) {
            return;
        }

        ticket.setEstadoTicket(ESTADO_RECHAZADO);
        ticketRevisionRepository.save(ticket);

        pagoTicketRevisionRepository.findByTicketIdTicketParaActualizar(idTicket).ifPresent(pago -> {
            if (PAGO_PENDIENTE.equals(pago.getEstadoPago())) {
                pago.setEstadoPago(PAGO_EXPIRADO);
                pagoTicketRevisionRepository.save(pago);
            }
        });

        log.info("[TicketRevisionExpiracionServicio] Ticket {} rechazado automáticamente: sin pago confirmado a tiempo",
                idTicket);

        Pedido pedido = ticket.getPedido();
        String mensaje = "Tu solicitud de revisión adicional del pedido \"" + pedido.getServicio().getTituloServicio()
                + "\" fue rechazada automáticamente por falta de pago.";
        notificacionService.notificar(pedido.getUsuarioCliente(), "TICKET_RECHAZADO", mensaje);
        notificacionService.notificar(pedido.getServicio().getPerfil().getUsuario(), "TICKET_RECHAZADO",
                "Un ticket de revisión sin pagar del pedido \"" + pedido.getServicio().getTituloServicio()
                        + "\" se rechazó automáticamente.");
    }
}
