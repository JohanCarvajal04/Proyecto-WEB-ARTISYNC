package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.pedido.RevisionTicket;
import uteq.edu.ec.artisync.repository.legal.RevisionTicketPaymentRepository;
import uteq.edu.ec.artisync.repository.pedido.RevisionTicketRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;

/**
 * Extraído de RevisionTicketExpirationScheduler para que REQUIRES_NEW funcione
 * de verdad (mismo motivo documentado en RaffleExecutorService).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevisionTicketExpirationService {

    private static final String ESTADO_ABIERTO = "Abierto";
    private static final String ESTADO_RECHAZADO = "Rechazado";
    private static final String PAGO_PENDIENTE = "Pendiente";
    private static final String PAGO_EXPIRADO = "Expirado";

    private final RevisionTicketRepository ticketRevisionRepository;
    private final RevisionTicketPaymentRepository pagoTicketRevisionRepository;
    private final NotificationService notificacionService;

    /**
     * Rechaza automáticamente un ticket de revisión sin pago confirmado a
     * tiempo y expira su pago asociado si seguía {@code Pendiente}. Relee el
     * ticket con lock antes de actuar por si el creador ya lo resolvió o el
     * webhook de PayPal ya confirmó el pago mientras tanto.
     *
     * @param idTicket identificador del ticket de revisión a expirar
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Auditable(accion = "TICKET_EXPIRAR", modulo = AuditModule.PEDIDOS,
            correoActor = "'sistema:scheduler'",
            entidad = "tickets_revision", idEntidad = "#idTicket")
    public void expirarTicket(Long idTicket) {
        // Relectura con lock: si el creador ya resolvió el ticket, o el
        // webhook de PayPal ya confirmó el pago, entre que el scheduler lo
        // leyó y esta transacción arrancó, no hay nada que hacer.
        RevisionTicket ticket = ticketRevisionRepository.findByIdParaActualizar(idTicket).orElse(null);
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

        log.info("[RevisionTicketExpirationService] Ticket {} rechazado automáticamente: sin pago confirmado a tiempo",
                idTicket);

        Order pedido = ticket.getPedido();
        String mensaje = "Tu solicitud de revisión adicional del pedido \"" + pedido.getServicio().getTituloServicio()
                + "\" fue rechazada automáticamente por falta de pago.";
        notificacionService.notify(pedido.getUsuarioCliente(), "TICKET_RECHAZADO", mensaje);
        notificacionService.notify(pedido.getServicio().getPerfil().getUsuario(), "TICKET_RECHAZADO",
                "Un ticket de revisión sin pagar del pedido \"" + pedido.getServicio().getTituloServicio()
                        + "\" se rechazó automáticamente.");
    }
}
