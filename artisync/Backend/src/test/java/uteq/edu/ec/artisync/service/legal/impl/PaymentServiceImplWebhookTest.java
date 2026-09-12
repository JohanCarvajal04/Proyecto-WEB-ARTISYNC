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
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.http.HttpMethod;
import org.springframework.test.util.ReflectionTestUtils;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;
import uteq.edu.ec.artisync.entity.legal.PaymentTransaction;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.legal.EscrowPaymentRepository;
import uteq.edu.ec.artisync.repository.legal.PaymentTransactionRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;
import uteq.edu.ec.artisync.service.legal.IRevisionTicketPaymentService;
import uteq.edu.ec.artisync.service.shared.paypal.PayPalClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RNF-14: el webhook de PayPal es público, así que la verificación de firma es
 * lo único que separa una notificación real de un POST falsificado que marque
 * un pedido como pagado. Estas pruebas fijan el comportamiento fail-closed.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplWebhookTest {

    @Mock private EscrowPaymentRepository pagoGarantiaRepository;
    @Mock private ContractRepository contratoRepository;
    @Mock private PaymentTransactionRepository transaccionPagoRepository;
    @Mock private NotificationService notificacionService;
    @Mock private IRevisionTicketPaymentService pagoTicketRevisionServicio;

    @Mock private PayPalClient payPalClient;

    @InjectMocks
    private PaymentServiceImpl pagoServicio;

    private EscrowPayment pagoPendiente;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String texto) {
        try {
            return MAPPER.readTree(texto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Payload con la forma real de CHECKOUT.ORDER.APPROVED. */
    private static final String EVENTO_APROBADO = """
        {
          "id": "WH-EVENTO-9999",
          "event_type": "CHECKOUT.ORDER.APPROVED",
          "resource": { "id": "ORDER-123" }
        }
        """;

    @BeforeEach
    void setUp() {
        User cliente = User.builder().idUsuario(100L).build();
        User creador = User.builder().idUsuario(200L).build();
        CreatorProfile perfil = CreatorProfile.builder().usuario(creador).build();
        Offering servicio = Offering.builder().perfil(perfil).tituloServicio("Offering de prueba").build();
        Order pedido = Order.builder().idPedido(1L).usuarioCliente(cliente).servicio(servicio).build();
        Contract contrato = Contract.builder().idContrato(5L).pedido(pedido).build();

        pagoPendiente = EscrowPayment.builder()
                .idPago(1L)
                .contrato(contrato)
                .idOrdenPaypal("ORDER-123")
                .montoRetenido(new BigDecimal("50.00"))
                .estadoFondos("Pendiente")
                .build();

        ReflectionTestUtils.setField(pagoServicio, "paypalWebhookId", "WEBHOOK-CONFIGURADO");
        given(pagoGarantiaRepository.findByIdOrdenPaypal("ORDER-123"))
                .willReturn(Optional.of(pagoPendiente));
    }

    @Test
    @DisplayName("sin webhook-id configurado no se confirma ningun pago")
    void sinWebhookIdNoConfirma() {
        ReflectionTestUtils.setField(pagoServicio, "paypalWebhookId", "");

        pagoServicio.processPayPalWebhook(EVENTO_APROBADO, "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
        verify(transaccionPagoRepository, never()).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("sin las cabeceras de firma se rechaza la notificacion")
    void sinCabecerasDeFirmaRechaza() {
        pagoServicio.processPayPalWebhook(EVENTO_APROBADO, null, null, null, null, null, null);

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
    }

    @Test
    @DisplayName("un payload ilegible no revienta ni confirma nada")
    void payloadIlegible() {
        pagoServicio.processPayPalWebhook("esto no es json", "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
    }

    @Test
    @DisplayName("si PayPal responde FAILURE a la verificacion, no se confirma")
    void firmaInvalidaNoConfirma() {
        conRespuestasPayPal("""
                {"verification_status":"FAILURE"}""");

        pagoServicio.processPayPalWebhook(EVENTO_APROBADO, "TX-1", "2026-01-01",
                "firma-falsa", "https://cert", "SHA256withRSA", "1.0");

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
    }

    /**
     * Camino completo: firma válida → captura → fondos retenidos.
     *
     * <p>Cubre además el id de la orden: el parser antiguo usaba
     * {@code indexOf("\"id\":\"")} y cogía WH-EVENTO-9999 (el id del evento) en
     * vez de ORDER-123, así que ni siquiera encontraba el pago.
     */
    @Test
    @DisplayName("firma valida y captura COMPLETED dejan los fondos retenidos")
    void firmaValidaCapturaYRetiene() {
        conRespuestasPayPal(
                """
                {"verification_status":"SUCCESS"}""",
                """
                {"status":"COMPLETED"}""");

        pagoServicio.processPayPalWebhook(EVENTO_APROBADO, "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Retenido");
        verify(pagoGarantiaRepository).save(pagoPendiente);
        verify(transaccionPagoRepository).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("si la captura no completa, los fondos NO se marcan retenidos")
    void capturaFallidaNoRetiene() {
        conRespuestasPayPal(
                """
                {"verification_status":"SUCCESS"}""",
                """
                {"status":"PAYER_ACTION_REQUIRED"}""");

        pagoServicio.processPayPalWebhook(EVENTO_APROBADO, "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
        verify(transaccionPagoRepository, never()).save(any(PaymentTransaction.class));
    }

    /** REQ-F-022b/c: la orden puede ser el cargo adicional de un ticket de revision, no un pago de garantia. */
    @Test
    @DisplayName("una orden que no es de pago de garantia se delega al pago de ticket de revision")
    void ordenDesconocida_seDelegaAPagoDeTicketRevision() {
        given(pagoGarantiaRepository.findByIdOrdenPaypal("ORDER-999")).willReturn(java.util.Optional.empty());
        given(pagoTicketRevisionServicio.processOrderWebhook("ORDER-999", "CHECKOUT.ORDER.APPROVED"))
                .willReturn(true);
        conRespuestasPayPal("""
                {"verification_status":"SUCCESS"}""");

        String eventoOrdenDesconocida = """
                {
                  "id": "WH-EVENTO-8888",
                  "event_type": "CHECKOUT.ORDER.APPROVED",
                  "resource": { "id": "ORDER-999" }
                }
                """;
        pagoServicio.processPayPalWebhook(eventoOrdenDesconocida, "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        verify(pagoTicketRevisionServicio).processOrderWebhook("ORDER-999", "CHECKOUT.ORDER.APPROVED");
        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
    }

    @Test
    @DisplayName("un reintento de PayPal no duplica la transaccion de ingreso")
    void reintentoNoDuplica() {
        pagoPendiente.setEstadoFondos("Retenido");
        conRespuestasPayPal("""
                {"verification_status":"SUCCESS"}""");

        pagoServicio.processPayPalWebhook(EVENTO_APROBADO, "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        verify(transaccionPagoRepository, never()).save(any(PaymentTransaction.class));
    }

    /**
     * Encola las respuestas de PayPal. La primera llamada de cada intercambio es
     * el token OAuth, que se resuelve aparte por devolver Map.
     */
    private void conRespuestasPayPal(String... cuerpos) {
        OngoingStubbing<JsonNode> stub = when(payPalClient.llamarPayPal(
                anyString(), any(HttpMethod.class), any(JsonNode.class)));
        for (String cuerpo : cuerpos) {
            stub = stub.thenReturn(json(cuerpo));
        }
    }
}
