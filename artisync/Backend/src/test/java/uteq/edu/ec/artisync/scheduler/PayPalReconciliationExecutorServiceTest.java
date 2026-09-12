package uteq.edu.ec.artisync.scheduler;

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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;
import uteq.edu.ec.artisync.entity.legal.PaymentTransaction;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.repository.legal.EscrowPaymentRepository;
import uteq.edu.ec.artisync.repository.legal.PaymentTransactionRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;
import uteq.edu.ec.artisync.service.shared.paypal.PayPalClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/** REQ-NF-019: cubre la tabla de decisión de la reconciliación activa contra PayPal. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PayPalReconciliationExecutorServiceTest {

    @Mock private EscrowPaymentRepository pagoGarantiaRepository;
    @Mock private PaymentTransactionRepository transaccionPagoRepository;
    @Mock private NotificationService notificacionService;
    @Mock private PayPalClient payPalClient;

    @InjectMocks
    private PayPalReconciliationExecutorService ejecutor;

    private EscrowPayment pagoPendiente;

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

        given(pagoGarantiaRepository.findByIdParaActualizar(1L)).willReturn(Optional.of(pagoPendiente));
    }

    @Test
    @DisplayName("orden COMPLETED en PayPal confirma el pago sin volver a capturar")
    void ordenCompletada_confirmaPago() {
        given(payPalClient.callPayPal(anyString(), eq(HttpMethod.GET), any()))
                .willReturn(json("""
                        {"status":"COMPLETED"}"""));

        ejecutor.reconciliar(1L);

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Retenido");
        verify(pagoGarantiaRepository).save(pagoPendiente);
        verify(transaccionPagoRepository).save(any(PaymentTransaction.class));
        verify(payPalClient, never()).callPayPal(contains("/capture"), any(), any());
    }

    @Test
    @DisplayName("orden APPROVED se captura de forma proactiva y luego se confirma")
    void ordenAprobada_capturaYConfirma() {
        given(payPalClient.callPayPal(eq("/v2/checkout/orders/ORDER-123"), eq(HttpMethod.GET), any()))
                .willReturn(json("""
                        {"status":"APPROVED"}"""));
        given(payPalClient.callPayPal(eq("/v2/checkout/orders/ORDER-123/capture"), eq(HttpMethod.POST), any()))
                .willReturn(json("""
                        {"status":"COMPLETED"}"""));

        ejecutor.reconciliar(1L);

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Retenido");
        verify(transaccionPagoRepository).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("orden ya capturada (ORDER_ALREADY_CAPTURED) tambien confirma el pago")
    void ordenYaCapturada_confirmaIgual() {
        given(payPalClient.callPayPal(eq("/v2/checkout/orders/ORDER-123"), eq(HttpMethod.GET), any()))
                .willReturn(json("""
                        {"status":"APPROVED"}"""));
        HttpStatusCodeException error = mock(HttpStatusCodeException.class);
        when(error.getResponseBodyAsString()).thenReturn("{\"name\":\"ORDER_ALREADY_CAPTURED\"}");
        given(payPalClient.callPayPal(eq("/v2/checkout/orders/ORDER-123/capture"), eq(HttpMethod.POST), any()))
                .willThrow(error);

        ejecutor.reconciliar(1L);

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Retenido");
        verify(transaccionPagoRepository).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("orden VOIDED no cambia el estado: el cliente debera iniciar un nuevo pago")
    void ordenVoided_noCambiaEstado() {
        given(payPalClient.callPayPal(anyString(), eq(HttpMethod.GET), any()))
                .willReturn(json("""
                        {"status":"VOIDED"}"""));

        ejecutor.reconciliar(1L);

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
    }

    @Test
    @DisplayName("orden todavia CREATED no hace nada, se reintenta en el proximo ciclo")
    void ordenTodaviaCreada_noHaceNada() {
        given(payPalClient.callPayPal(anyString(), eq(HttpMethod.GET), any()))
                .willReturn(json("""
                        {"status":"CREATED"}"""));

        ejecutor.reconciliar(1L);

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
    }

    @Test
    @DisplayName("si el webhook ya confirmo el pago entre la lectura del scheduler y esta transaccion, no hace nada")
    void pagoYaResueltoPorWebhook_noOp() {
        pagoPendiente.setEstadoFondos("Retenido");

        ejecutor.reconciliar(1L);

        verifyNoInteractions(payPalClient);
        verify(pagoGarantiaRepository, never()).save(any());
    }
}
