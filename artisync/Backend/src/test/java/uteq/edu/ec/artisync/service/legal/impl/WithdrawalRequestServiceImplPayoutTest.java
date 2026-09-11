package uteq.edu.ec.artisync.service.legal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.OngoingStubbing;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import uteq.edu.ec.artisync.dto.respuesta.legal.WithdrawalRequestResponse;
import uteq.edu.ec.artisync.entity.legal.WithdrawalRequest;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.repository.legal.WithdrawalRequestRepository;
import uteq.edu.ec.artisync.repository.legal.PaymentTransactionRepository;
import uteq.edu.ec.artisync.repository.perfil.DatosPagoCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.shared.paypal.PayPalClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Fase 2: la llamada real a PayPal Payouts dentro de aprobar()/reintentar().
 * PayPalClient mockeado, sin salir a la red, encolando las respuestas de la
 * propia llamada (el paso de OAuth queda encapsulado dentro del cliente).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WithdrawalRequestServiceImplPayoutTest {

    private static final Long ID_ADMIN = 900L;

    @Mock private WithdrawalRequestRepository solicitudRetiroRepository;
    @Mock private DatosPagoCreadorRepository datosPagoCreadorRepository;
    @Mock private PaymentTransactionRepository transaccionPagoRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private PayPalClient payPalClient;

    @InjectMocks
    private WithdrawalRequestServiceImpl servicio;

    private WithdrawalRequest solicitudPendiente;

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
        User creador = User.builder().idUsuario(200L).nombres("Ana").apellidos("Creadora").build();
        User admin = User.builder().idUsuario(ID_ADMIN).nombres("Admin").apellidos("X").build();

        solicitudPendiente = WithdrawalRequest.builder()
                .idSolicitud(1L)
                .usuarioCreador(creador)
                .montoSolicitado(new BigDecimal("20.00"))
                .correoPaypalDestino("ana@paypal.test")
                .estado("Pendiente")
                .build();

        given(solicitudRetiroRepository.findByIdParaActualizar(1L)).willReturn(Optional.of(solicitudPendiente));
        given(solicitudRetiroRepository.save(any(WithdrawalRequest.class))).willAnswer(inv -> inv.getArgument(0));
        given(usuarioRepository.findById(ID_ADMIN)).willReturn(Optional.of(admin));
    }

    /** Encola las respuestas de PayPal, una por cada llamada a ejecutarPayout(). */
    private void conRespuestasPayPal(String... cuerpos) {
        OngoingStubbing<JsonNode> stub = when(payPalClient.llamarPayPal(
                anyString(), any(HttpMethod.class), any(JsonNode.class)));
        for (String cuerpo : cuerpos) {
            stub = stub.thenReturn(json(cuerpo));
        }
    }

    @Test
    @DisplayName("batch_status SUCCESS deja la solicitud Pagada")
    void aprobar_conRespuestaSuccess_quedaPagada() {
        conRespuestasPayPal("""
                {"batch_header":{"batch_status":"SUCCESS","payout_batch_id":"PAYOUTBATCH-1"}}""");

        WithdrawalRequestResponse respuesta = servicio.aprobar(1L, ID_ADMIN);

        assertThat(respuesta.estado()).isEqualTo("Pagado");
        assertThat(respuesta.idPayoutPaypal()).isEqualTo("PAYOUTBATCH-1");
        assertThat(respuesta.fechaPago()).isNotNull();
        assertThat(respuesta.mensajeError()).isNull();
    }

    @Test
    @DisplayName("batch_status PENDING deja la solicitud en Aprobado (PayPal la resuelve después)")
    void aprobar_conRespuestaPending_quedaAprobada() {
        conRespuestasPayPal("""
                {"batch_header":{"batch_status":"PENDING","payout_batch_id":"PAYOUTBATCH-2"}}""");

        WithdrawalRequestResponse respuesta = servicio.aprobar(1L, ID_ADMIN);

        assertThat(respuesta.estado()).isEqualTo("Aprobado");
        assertThat(respuesta.fechaPago()).isNull();
    }

    @Test
    @DisplayName("batch_status DENIED deja la solicitud Fallida con el mensaje de error")
    void aprobar_conRespuestaDenied_quedaFallida() {
        conRespuestasPayPal("""
                {"batch_header":{"batch_status":"DENIED","payout_batch_id":"PAYOUTBATCH-3"}}""");

        WithdrawalRequestResponse respuesta = servicio.aprobar(1L, ID_ADMIN);

        assertThat(respuesta.estado()).isEqualTo("Fallido");
        assertThat(respuesta.mensajeError()).contains("DENIED");
    }

    @Test
    @DisplayName("un error HTTP de PayPal deja la solicitud Fallida, sin propagar la excepcion")
    void aprobar_conErrorHttp_quedaFallida() {
        HttpStatusCodeException error = mock(HttpStatusCodeException.class);
        when(error.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(error.getResponseBodyAsString()).thenReturn("{\"name\":\"RECEIVER_UNREGISTERED\"}");
        given(payPalClient.llamarPayPal(anyString(), eq(HttpMethod.POST), any(JsonNode.class)))
                .willThrow(error);

        WithdrawalRequestResponse respuesta = servicio.aprobar(1L, ID_ADMIN);

        assertThat(respuesta.estado()).isEqualTo("Fallido");
        assertThat(respuesta.mensajeError()).contains("RECEIVER_UNREGISTERED");
    }

    @Test
    @DisplayName("reintentar reusa el mismo sender_batch_id del intento original")
    void reintentar_reusaElMismoSenderBatchId() {
        conRespuestasPayPal(
                """
                {"batch_header":{"batch_status":"DENIED","payout_batch_id":"PAYOUTBATCH-4"}}""",
                """
                {"batch_header":{"batch_status":"SUCCESS","payout_batch_id":"PAYOUTBATCH-4"}}""");

        WithdrawalRequestResponse primerIntento = servicio.aprobar(1L, ID_ADMIN);
        assertThat(primerIntento.estado()).isEqualTo("Fallido");

        // reintentar() exige que la solicitud esté en Fallido: refleja en el mock
        // el estado que quedó tras la primera llamada.
        solicitudPendiente.setEstado("Fallido");

        WithdrawalRequestResponse segundoIntento = servicio.reintentar(1L, ID_ADMIN);
        assertThat(segundoIntento.estado()).isEqualTo("Pagado");

        ArgumentCaptor<JsonNode> captor = ArgumentCaptor.forClass(JsonNode.class);
        org.mockito.Mockito.verify(payPalClient, org.mockito.Mockito.times(2)).llamarPayPal(
                contains("/v1/payments/payouts"), eq(HttpMethod.POST), captor.capture());

        String cuerpoPrimero = captor.getAllValues().get(0).toString();
        String cuerpoSegundo = captor.getAllValues().get(1).toString();
        assertThat(cuerpoPrimero).contains("\"sender_batch_id\":\"retiro-1\"");
        assertThat(cuerpoSegundo).contains("\"sender_batch_id\":\"retiro-1\"");
    }

    @Test
    @DisplayName("reintentar solo esta permitido si la solicitud esta Fallida")
    void reintentar_solicitudNoFallida_rechaza() {
        assertThat(solicitudPendiente.getEstado()).isEqualTo("Pendiente");

        org.junit.jupiter.api.Assertions.assertThrows(
                uteq.edu.ec.artisync.exception.BusinessRuleException.class,
                () -> servicio.reintentar(1L, ID_ADMIN));
    }
}
