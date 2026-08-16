package uteq.edu.ec.artisync.service.legal.impl;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;
import uteq.edu.ec.artisync.entity.legal.TransaccionPago;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.legal.PagoGarantiaRepository;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
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
class PagoServicioImplWebhookTest {

    @Mock private PagoGarantiaRepository pagoGarantiaRepository;
    @Mock private ContratoRepository contratoRepository;
    @Mock private TransaccionPagoRepository transaccionPagoRepository;

    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private PagoServicioImpl pagoServicio;

    private PagoGarantia pagoPendiente;

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
        pagoPendiente = PagoGarantia.builder()
                .idPago(1L)
                .idOrdenPaypal("ORDER-123")
                .montoRetenido(new BigDecimal("50.00"))
                .estadoFondos("Pendiente")
                .build();

        ReflectionTestUtils.setField(pagoServicio, "paypalWebhookId", "WEBHOOK-CONFIGURADO");
        // @InjectMocks no sustituye el campo inicializado en línea; se fuerza.
        ReflectionTestUtils.setField(pagoServicio, "restTemplate", restTemplate);
        given(pagoGarantiaRepository.findByIdOrdenPaypal("ORDER-123"))
                .willReturn(Optional.of(pagoPendiente));
    }

    @Test
    @DisplayName("sin webhook-id configurado no se confirma ningun pago")
    void sinWebhookIdNoConfirma() {
        ReflectionTestUtils.setField(pagoServicio, "paypalWebhookId", "");

        pagoServicio.procesarWebhookPayPal(EVENTO_APROBADO, "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
        verify(transaccionPagoRepository, never()).save(any(TransaccionPago.class));
    }

    @Test
    @DisplayName("sin las cabeceras de firma se rechaza la notificacion")
    void sinCabecerasDeFirmaRechaza() {
        pagoServicio.procesarWebhookPayPal(EVENTO_APROBADO, null, null, null, null, null, null);

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
    }

    @Test
    @DisplayName("un payload ilegible no revienta ni confirma nada")
    void payloadIlegible() {
        pagoServicio.procesarWebhookPayPal("esto no es json", "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
    }

    @Test
    @DisplayName("si PayPal responde FAILURE a la verificacion, no se confirma")
    void firmaInvalidaNoConfirma() {
        conRespuestasPayPal("""
                {"verification_status":"FAILURE"}""");

        pagoServicio.procesarWebhookPayPal(EVENTO_APROBADO, "TX-1", "2026-01-01",
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

        pagoServicio.procesarWebhookPayPal(EVENTO_APROBADO, "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Retenido");
        verify(pagoGarantiaRepository).save(pagoPendiente);
        verify(transaccionPagoRepository).save(any(TransaccionPago.class));
    }

    @Test
    @DisplayName("si la captura no completa, los fondos NO se marcan retenidos")
    void capturaFallidaNoRetiene() {
        conRespuestasPayPal(
                """
                {"verification_status":"SUCCESS"}""",
                """
                {"status":"PAYER_ACTION_REQUIRED"}""");

        pagoServicio.procesarWebhookPayPal(EVENTO_APROBADO, "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        assertThat(pagoPendiente.getEstadoFondos()).isEqualTo("Pendiente");
        verify(pagoGarantiaRepository, never()).save(any());
        verify(transaccionPagoRepository, never()).save(any(TransaccionPago.class));
    }

    @Test
    @DisplayName("un reintento de PayPal no duplica la transaccion de ingreso")
    void reintentoNoDuplica() {
        pagoPendiente.setEstadoFondos("Retenido");
        conRespuestasPayPal("""
                {"verification_status":"SUCCESS"}""");

        pagoServicio.procesarWebhookPayPal(EVENTO_APROBADO, "TX-1", "2026-01-01",
                "firma", "https://cert", "SHA256withRSA", "1.0");

        verify(transaccionPagoRepository, never()).save(any(TransaccionPago.class));
    }

    /**
     * Encola las respuestas de PayPal. La primera llamada de cada intercambio es
     * el token OAuth, que se resuelve aparte por devolver Map.
     */
    private void conRespuestasPayPal(String... cuerpos) {
        given(restTemplate.exchange(contains("/v1/oauth2/token"), any(HttpMethod.class),
                any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("access_token", "token-de-prueba")));

        OngoingStubbing<ResponseEntity<String>> stub = when(restTemplate.exchange(
                anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)));
        for (String cuerpo : cuerpos) {
            stub = stub.thenReturn(ResponseEntity.ok(cuerpo));
        }
    }
}
