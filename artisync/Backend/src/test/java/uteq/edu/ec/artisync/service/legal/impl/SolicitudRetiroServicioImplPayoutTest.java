package uteq.edu.ec.artisync.service.legal.impl;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaSolicitudRetiro;
import uteq.edu.ec.artisync.entity.legal.SolicitudRetiro;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.legal.SolicitudRetiroRepository;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;
import uteq.edu.ec.artisync.repository.perfil.DatosPagoCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;

import java.math.BigDecimal;
import java.util.Map;
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
 * Mismo patrón que PagoServicioImplWebhookTest: RestTemplate mockeado, sin
 * salir a la red, encolando las respuestas de OAuth y de la propia llamada.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudRetiroServicioImplPayoutTest {

    private static final Long ID_ADMIN = 900L;

    @Mock private SolicitudRetiroRepository solicitudRetiroRepository;
    @Mock private DatosPagoCreadorRepository datosPagoCreadorRepository;
    @Mock private TransaccionPagoRepository transaccionPagoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private SolicitudRetiroServicioImpl servicio;

    private SolicitudRetiro solicitudPendiente;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(servicio, "restTemplate", restTemplate);

        Usuario creador = Usuario.builder().idUsuario(200L).nombres("Ana").apellidos("Creadora").build();
        Usuario admin = Usuario.builder().idUsuario(ID_ADMIN).nombres("Admin").apellidos("X").build();

        solicitudPendiente = SolicitudRetiro.builder()
                .idSolicitud(1L)
                .usuarioCreador(creador)
                .montoSolicitado(new BigDecimal("20.00"))
                .correoPaypalDestino("ana@paypal.test")
                .estado("Pendiente")
                .build();

        given(solicitudRetiroRepository.findByIdParaActualizar(1L)).willReturn(Optional.of(solicitudPendiente));
        given(solicitudRetiroRepository.save(any(SolicitudRetiro.class))).willAnswer(inv -> inv.getArgument(0));
        given(usuarioRepository.findById(ID_ADMIN)).willReturn(Optional.of(admin));
    }

    /** Encola las respuestas de PayPal. La primera llamada de cada intercambio es el token OAuth. */
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

    @Test
    @DisplayName("batch_status SUCCESS deja la solicitud Pagada")
    void aprobar_conRespuestaSuccess_quedaPagada() {
        conRespuestasPayPal("""
                {"batch_header":{"batch_status":"SUCCESS","payout_batch_id":"PAYOUTBATCH-1"}}""");

        RespuestaSolicitudRetiro respuesta = servicio.aprobar(1L, ID_ADMIN);

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

        RespuestaSolicitudRetiro respuesta = servicio.aprobar(1L, ID_ADMIN);

        assertThat(respuesta.estado()).isEqualTo("Aprobado");
        assertThat(respuesta.fechaPago()).isNull();
    }

    @Test
    @DisplayName("batch_status DENIED deja la solicitud Fallida con el mensaje de error")
    void aprobar_conRespuestaDenied_quedaFallida() {
        conRespuestasPayPal("""
                {"batch_header":{"batch_status":"DENIED","payout_batch_id":"PAYOUTBATCH-3"}}""");

        RespuestaSolicitudRetiro respuesta = servicio.aprobar(1L, ID_ADMIN);

        assertThat(respuesta.estado()).isEqualTo("Fallido");
        assertThat(respuesta.mensajeError()).contains("DENIED");
    }

    @Test
    @DisplayName("un error HTTP de PayPal deja la solicitud Fallida, sin propagar la excepcion")
    void aprobar_conErrorHttp_quedaFallida() {
        given(restTemplate.exchange(contains("/v1/oauth2/token"), any(HttpMethod.class),
                any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("access_token", "token-de-prueba")));

        HttpStatusCodeException error = mock(HttpStatusCodeException.class);
        when(error.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(error.getResponseBodyAsString()).thenReturn("{\"name\":\"RECEIVER_UNREGISTERED\"}");
        given(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .willThrow(error);

        RespuestaSolicitudRetiro respuesta = servicio.aprobar(1L, ID_ADMIN);

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

        RespuestaSolicitudRetiro primerIntento = servicio.aprobar(1L, ID_ADMIN);
        assertThat(primerIntento.estado()).isEqualTo("Fallido");

        // reintentar() exige que la solicitud esté en Fallido: refleja en el mock
        // el estado que quedó tras la primera llamada.
        solicitudPendiente.setEstado("Fallido");

        RespuestaSolicitudRetiro segundoIntento = servicio.reintentar(1L, ID_ADMIN);
        assertThat(segundoIntento.estado()).isEqualTo("Pagado");

        ArgumentCaptor<HttpEntity<String>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        org.mockito.Mockito.verify(restTemplate, org.mockito.Mockito.times(2)).exchange(
                contains("/v1/payments/payouts"), eq(HttpMethod.POST), captor.capture(), eq(String.class));

        String cuerpoPrimero = captor.getAllValues().get(0).getBody();
        String cuerpoSegundo = captor.getAllValues().get(1).getBody();
        assertThat(cuerpoPrimero).contains("\"sender_batch_id\":\"retiro-1\"");
        assertThat(cuerpoSegundo).contains("\"sender_batch_id\":\"retiro-1\"");
    }

    @Test
    @DisplayName("reintentar solo esta permitido si la solicitud esta Fallida")
    void reintentar_solicitudNoFallida_rechaza() {
        assertThat(solicitudPendiente.getEstado()).isEqualTo("Pendiente");

        org.junit.jupiter.api.Assertions.assertThrows(
                uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio.class,
                () -> servicio.reintentar(1L, ID_ADMIN));
    }
}
