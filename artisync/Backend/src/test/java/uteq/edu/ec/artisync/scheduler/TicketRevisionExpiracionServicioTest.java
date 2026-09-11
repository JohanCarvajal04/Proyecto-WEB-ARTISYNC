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
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.PagoTicketRevision;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.TicketRevision;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.legal.PagoTicketRevisionRepository;
import uteq.edu.ec.artisync.repository.pedido.TicketRevisionRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

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
class TicketRevisionExpiracionServicioTest {

    @Mock private TicketRevisionRepository ticketRevisionRepository;
    @Mock private PagoTicketRevisionRepository pagoTicketRevisionRepository;
    @Mock private NotificacionService notificacionService;

    @InjectMocks
    private TicketRevisionExpiracionServicio servicio;

    private TicketRevision ticketAbierto;

    @BeforeEach
    void setUp() {
        Usuario cliente = Usuario.builder().idUsuario(100L).build();
        Usuario creador = Usuario.builder().idUsuario(200L).build();
        PerfilCreador perfil = PerfilCreador.builder().usuario(creador).build();
        Servicio servicioCatalogo = Servicio.builder().perfil(perfil).tituloServicio("Servicio de prueba").build();
        Pedido pedido = Pedido.builder().idPedido(1L).usuarioCliente(cliente).servicio(servicioCatalogo).build();

        ticketAbierto = TicketRevision.builder()
                .idTicket(9L).pedido(pedido).estadoTicket("Abierto")
                .costoAdicionalGenerado(new BigDecimal("5.00")).build();

        given(ticketRevisionRepository.findByIdParaActualizar(9L)).willReturn(Optional.of(ticketAbierto));
    }

    @Test
    @DisplayName("marca el ticket Rechazado y el pago pendiente Expirado")
    void marcaRechazado_yExpiraPago() {
        PagoTicketRevision pagoPendiente = PagoTicketRevision.builder()
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
