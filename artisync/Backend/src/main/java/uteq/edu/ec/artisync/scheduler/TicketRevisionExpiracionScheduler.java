package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.pedido.RevisionTicket;
import uteq.edu.ec.artisync.repository.pedido.RevisionTicketRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REQ-F-022c: rechaza automáticamente un ticket de revisión que generó cargo
 * adicional y sigue sin pago confirmado más allá de un umbral configurable.
 *
 * Requiere: @EnableScheduling en ArtisyncApplication (ya presente). Mismo
 * motivo que ReconciliacionPayPalScheduler para usar @Value en vez de una
 * constante hardcodeada: se prefiere consistencia con el resto de umbrales
 * nuevos de esta ronda, aunque el texto del requisito no lo exija de forma
 * tan explícita como REQ-NF-019.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TicketRevisionExpiracionScheduler {

    private final RevisionTicketRepository ticketRevisionRepository;
    private final TicketRevisionExpiracionServicio ticketRevisionExpiracionServicio;

    @Value("${ticketrevision.expiracion-horas:48}")
    private int expiracionHoras;

    @Scheduled(fixedRateString = "${ticketrevision.expiracion.intervalo-ms:1800000}") // 30 min
    public void expirarTicketsSinPagar() {
        LocalDateTime limite = LocalDateTime.now().minusHours(expiracionHoras);
        List<RevisionTicket> vencidos = ticketRevisionRepository.findVencidosSinPagoConfirmado(limite);

        if (vencidos.isEmpty()) {
            return;
        }

        log.info("[TicketRevisionExpiracionScheduler] Rechazando {} ticket(s) sin pagar de más de {}h",
                vencidos.size(), expiracionHoras);

        for (RevisionTicket ticket : vencidos) {
            try {
                ticketRevisionExpiracionServicio.expirarTicket(ticket.getIdTicket());
            } catch (Exception e) {
                log.error("[TicketRevisionExpiracionScheduler] Error expirando ticket {}: {}",
                        ticket.getIdTicket(), e.getMessage(), e);
            }
        }
    }
}
