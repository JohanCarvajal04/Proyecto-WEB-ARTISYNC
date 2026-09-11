package uteq.edu.ec.artisync.service.legal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.PagoTicketRevision;
import uteq.edu.ec.artisync.entity.pedido.MotivoRechazo;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.TicketRevision;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.repository.legal.PagoTicketRevisionRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;
import uteq.edu.ec.artisync.service.shared.paypal.PayPalClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** REQ-F-022b/c: pago del cargo adicional de un ticket de revision. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoTicketRevisionServicioImplTest {

    @Mock private PagoTicketRevisionRepository pagoTicketRevisionRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private PayPalClient payPalClient;

    @InjectMocks
    private PagoTicketRevisionServicioImpl servicio;

    private TicketRevision ticket;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String texto) {
        try {
            return MAPPER.readTree(texto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    void setUp() {
        User cliente = User.builder().idUsuario(100L).build();
        User creador = User.builder().idUsuario(200L).build();
        PerfilCreador perfil = PerfilCreador.builder().usuario(creador).build();
        Servicio servicioCatalogo = Servicio.builder().perfil(perfil).tituloServicio("Servicio de prueba").build();
        Pedido pedido = Pedido.builder().idPedido(1L).usuarioCliente(cliente).servicio(servicioCatalogo).build();
        MotivoRechazo motivo = MotivoRechazo.builder().idMotivo(1L).build();

        ticket = TicketRevision.builder()
                .idTicket(9L)
                .pedido(pedido)
                .motivo(motivo)
                .descripcionCliente("no cumple")
                .costoAdicionalGenerado(new BigDecimal("5.00"))
                .build();

        ReflectionTestUtils.setField(servicio, "frontendUrl", "http://localhost:4200");
    }

    @Test
    @DisplayName("crea y persiste la orden de pago con la url de aprobacion de PayPal")
    void creaYPersisteOrden_ok() {
        given(pagoTicketRevisionRepository.findByTicketIdTicket(9L)).willReturn(Optional.empty());
        given(payPalClient.llamarPayPal(anyString(), any(HttpMethod.class), any()))
                .willReturn(json("""
                        {"id":"ORDER-TICKET-1","links":[{"rel":"approve","href":"https://paypal/approve/1"}]}"""));

        servicio.crearOrdenPago(ticket);

        var captor = org.mockito.ArgumentCaptor.forClass(PagoTicketRevision.class);
        verify(pagoTicketRevisionRepository).save(captor.capture());
        PagoTicketRevision guardado = captor.getValue();
        assertThat(guardado.getIdOrdenPaypal()).isEqualTo("ORDER-TICKET-1");
        assertThat(guardado.getUrlAprobacion()).isEqualTo("https://paypal/approve/1");
        assertThat(guardado.getEstadoPago()).isEqualTo("Pendiente");
        assertThat(guardado.getMonto()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("reutiliza la fila existente en vez de crear una nueva si el ticket ya tenia un intento")
    void reutilizaFilaExistente_ok() {
        PagoTicketRevision existente = PagoTicketRevision.builder()
                .idPagoTicket(3L).ticket(ticket).monto(new BigDecimal("5.00")).estadoPago("Pendiente").build();
        given(pagoTicketRevisionRepository.findByTicketIdTicket(9L)).willReturn(Optional.of(existente));
        given(payPalClient.llamarPayPal(anyString(), any(HttpMethod.class), any()))
                .willReturn(json("""
                        {"id":"ORDER-TICKET-2","links":[{"rel":"approve","href":"https://paypal/approve/2"}]}"""));

        servicio.crearOrdenPago(ticket);

        verify(pagoTicketRevisionRepository).save(existente);
        assertThat(existente.getIdOrdenPaypal()).isEqualTo("ORDER-TICKET-2");
    }

    @Test
    @DisplayName("si PayPal falla al crear la orden, no revienta ni persiste nada")
    void fallaPaypalAlCrear_noRevienta() {
        given(pagoTicketRevisionRepository.findByTicketIdTicket(9L)).willReturn(Optional.empty());
        given(payPalClient.llamarPayPal(anyString(), any(HttpMethod.class), any()))
                .willThrow(new RuntimeException("timeout"));

        servicio.crearOrdenPago(ticket);

        verify(pagoTicketRevisionRepository, never()).save(any());
    }

    @Test
    @DisplayName("obtenerUrlPagoPendiente devuelve null si el pago ya esta pagado")
    void obtenerUrlPagoPendiente_nullSiPagado() {
        PagoTicketRevision pagado = PagoTicketRevision.builder()
                .ticket(ticket).monto(BigDecimal.TEN).estadoPago("Pagado").urlAprobacion("https://x").build();
        given(pagoTicketRevisionRepository.findByTicketIdTicket(9L)).willReturn(Optional.of(pagado));

        assertThat(servicio.obtenerUrlPagoPendiente(9L)).isNull();
    }

    @Test
    @DisplayName("obtenerUrlPagoPendiente devuelve la url si el pago sigue pendiente")
    void obtenerUrlPagoPendiente_devuelveUrlSiPendiente() {
        PagoTicketRevision pendiente = PagoTicketRevision.builder()
                .ticket(ticket).monto(BigDecimal.TEN).estadoPago("Pendiente").urlAprobacion("https://x").build();
        given(pagoTicketRevisionRepository.findByTicketIdTicket(9L)).willReturn(Optional.of(pendiente));

        assertThat(servicio.obtenerUrlPagoPendiente(9L)).isEqualTo("https://x");
    }

    @Test
    @DisplayName("procesarWebhookOrden devuelve false si la orden no es de un ticket de revision")
    void procesarWebhookOrden_ordenAjena_false() {
        given(pagoTicketRevisionRepository.findByIdOrdenPaypal("ORDER-AJENA")).willReturn(Optional.empty());

        boolean procesado = servicio.procesarWebhookOrden("ORDER-AJENA", "CHECKOUT.ORDER.APPROVED");

        assertThat(procesado).isFalse();
        verify(payPalClient, never()).llamarPayPal(anyString(), any(), any());
    }

    @Test
    @DisplayName("procesarWebhookOrden captura la orden aprobada y marca el pago como Pagado")
    void procesarWebhookOrden_aprobada_capturaMarcaPagado() {
        PagoTicketRevision pendiente = PagoTicketRevision.builder()
                .ticket(ticket).idOrdenPaypal("ORDER-TICKET-3").monto(new BigDecimal("5.00")).estadoPago("Pendiente").build();
        given(pagoTicketRevisionRepository.findByIdOrdenPaypal("ORDER-TICKET-3")).willReturn(Optional.of(pendiente));
        given(payPalClient.llamarPayPal(anyString(), any(HttpMethod.class), any()))
                .willReturn(json("""
                        {"status":"COMPLETED"}"""));

        boolean procesado = servicio.procesarWebhookOrden("ORDER-TICKET-3", "CHECKOUT.ORDER.APPROVED");

        assertThat(procesado).isTrue();
        assertThat(pendiente.getEstadoPago()).isEqualTo("Pagado");
        verify(pagoTicketRevisionRepository).save(pendiente);
    }

    @Test
    @DisplayName("procesarWebhookOrden no reprocesa un pago que ya estaba Pagado (idempotencia)")
    void procesarWebhookOrden_duplicado_noReprocesa() {
        PagoTicketRevision pagado = PagoTicketRevision.builder()
                .ticket(ticket).idOrdenPaypal("ORDER-TICKET-4").monto(new BigDecimal("5.00")).estadoPago("Pagado").build();
        given(pagoTicketRevisionRepository.findByIdOrdenPaypal("ORDER-TICKET-4")).willReturn(Optional.of(pagado));

        boolean procesado = servicio.procesarWebhookOrden("ORDER-TICKET-4", "PAYMENT.CAPTURE.COMPLETED");

        assertThat(procesado).isTrue();
        verify(payPalClient, never()).llamarPayPal(anyString(), any(), any());
        verify(pagoTicketRevisionRepository, never()).save(any());
    }
}
