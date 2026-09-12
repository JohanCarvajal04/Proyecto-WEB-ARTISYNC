package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.legal.RevisionTicketPayment;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.pedido.RevisionTicket;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.repository.legal.RevisionTicketPaymentRepository;
import uteq.edu.ec.artisync.repository.pedido.RevisionTicketRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** REQ-F-022c: cubre el rechazo automatico de un ticket de revision sin pagar. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RevisionTicketExpirationServiceTest {

    @Mock private RevisionTicketRepository ticketRevisionRepository;
    @Mock private RevisionTicketPaymentRepository pagoTicketRevisionRepository;
    @Mock private NotificationService notificacionService;

    @InjectMocks
    private RevisionTicketExpirationService servicio;

    private RevisionTicket ticketAbierto;

    @BeforeEach
    void setUp() {
        User cliente = User.builder().idUsuario(100L).build();
        User creador = User.builder().idUsuario(200L).build();
        CreatorProfile perfil = CreatorProfile.builder().usuario(creador).build();
        Offering servicioCatalogo = Offering.builder().perfil(perfil).tituloServicio("Offering de prueba").build();
        Order pedido = Order.builder().idPedido(1L).usuarioCliente(cliente).servicio(servicioCatalogo).build();

        ticketAbierto = RevisionTicket.builder()
                .idTicket(9L).pedido(pedido).estadoTicket("Abierto")
                .costoAdicionalGenerado(new BigDecimal("5.00")).build();

        given(ticketRevisionRepository.findByIdParaActualizar(9L)).willReturn(Optional.of(ticketAbierto));
    }

    @Test
    @DisplayName("marca el ticket Rechazado y el pago pendiente Expirado")
    void marcaRechazado_yExpiraPago() {
        RevisionTicketPayment pagoPendiente = RevisionTicketPayment.builder()
                .ticket(ticketAbierto).monto(new BigDecimal("5.00")).estadoPago("Pendiente").build();
        given(pagoTicketRevisionRepository.findByTicketIdTicketParaActualizar(9L))
                .willReturn(Optional.of(pagoPendiente));

        servicio.expirarTicket(9L);

        assertThat(ticketAbierto.getEstadoTicket()).isEqualTo("Rechazado");
        assertThat(pagoPendiente.getEstadoPago()).isEqualTo("Expirado");
        verify(ticketRevisionRepository).save(ticketAbierto);
        verify(pagoTicketRevisionRepository).save(pagoPendiente);
    }

    @Test
    @DisplayName("si el ticket ya fue resuelto (creador o webhook) entre la lectura del scheduler y esta transaccion, no hace nada")
    void yaResuelto_noOp() {
        ticketAbierto.setEstadoTicket("Resuelto");

        servicio.expirarTicket(9L);

        verify(ticketRevisionRepository, never()).save(any());
        verify(pagoTicketRevisionRepository, never()).save(any());
    }

    @Test
    @DisplayName("si no existe fila de pago asociada, igual rechaza el ticket sin fallar")
    void sinFilaDePago_rechazaTicketIgual() {
        given(pagoTicketRevisionRepository.findByTicketIdTicketParaActualizar(9L)).willReturn(Optional.empty());

        servicio.expirarTicket(9L);

        assertThat(ticketAbierto.getEstadoTicket()).isEqualTo("Rechazado");
        verify(pagoTicketRevisionRepository, never()).save(any());
    }
}
