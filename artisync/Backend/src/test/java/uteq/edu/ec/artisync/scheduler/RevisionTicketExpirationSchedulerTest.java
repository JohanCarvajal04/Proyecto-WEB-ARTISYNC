package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uteq.edu.ec.artisync.entity.order.RevisionTicket;
import uteq.edu.ec.artisync.repository.order.RevisionTicketRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** REQ-F-022c: mismo patron de prueba que VerificacionSchedulerTest / ReconciliacionPayPalSchedulerTest. */
@ExtendWith(MockitoExtension.class)
class RevisionTicketExpirationSchedulerTest {

    @Mock private RevisionTicketRepository ticketRevisionRepository;
    @Mock private RevisionTicketExpirationService ticketRevisionExpiracionServicio;

    @InjectMocks
    private RevisionTicketExpirationScheduler scheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(scheduler, "expiracionHoras", 48);
    }

    @Test
    void expirarTicketsSinPagar_delegaCadaTicketAlServicioDeExpiracion() {
        RevisionTicket vencido = RevisionTicket.builder().idTicket(1L).estadoTicket("Abierto").build();
        when(ticketRevisionRepository.findVencidosSinPagoConfirmado(any())).thenReturn(List.of(vencido));

        scheduler.expireUnpaidTickets();

        verify(ticketRevisionExpiracionServicio).expireTicket(1L);
    }

    @Test
    void expirarTicketsSinPagar_sinTicketsVencidos_noHaceNada() {
        when(ticketRevisionRepository.findVencidosSinPagoConfirmado(any())).thenReturn(List.of());

        scheduler.expireUnpaidTickets();

        verifyNoInteractions(ticketRevisionExpiracionServicio);
    }

    @Test
    void expirarTicketsSinPagar_unTicketFallaOtroSigueLogueaYContinua() {
        RevisionTicket a = RevisionTicket.builder().idTicket(1L).estadoTicket("Abierto").build();
        RevisionTicket b = RevisionTicket.builder().idTicket(2L).estadoTicket("Abierto").build();
        when(ticketRevisionRepository.findVencidosSinPagoConfirmado(any())).thenReturn(List.of(a, b));
        doThrow(new RuntimeException("fallo simulado")).when(ticketRevisionExpiracionServicio).expireTicket(1L);

        scheduler.expireUnpaidTickets();

        verify(ticketRevisionExpiracionServicio).expireTicket(1L);
        verify(ticketRevisionExpiracionServicio).expireTicket(2L);
    }
}
