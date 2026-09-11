package uteq.edu.ec.artisync.service.legal.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpStatusCodeException;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;
import uteq.edu.ec.artisync.entity.legal.PaymentTransaction;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.legal.EscrowPaymentRepository;
import uteq.edu.ec.artisync.repository.legal.PaymentTransactionRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;
import uteq.edu.ec.artisync.service.shared.paypal.PayPalClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REQ-NF-019: antes de esto no existía ninguna función para cancelar un
 * pedido con fondos ya retenidos en escrow. Estas pruebas cubren el reembolso
 * (cliente), la liberación por cancelación (solo admin), y los rechazos de
 * autorización y de estado.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplCancellationTest {

    @Mock private EscrowPaymentRepository pagoGarantiaRepository;
    @Mock private ContractRepository contratoRepository;
    @Mock private PaymentTransactionRepository transaccionPagoRepository;
    @Mock private NotificationService notificacionService;
    @Mock private PayPalClient payPalClient;

    @InjectMocks
    private PaymentServiceImpl pagoServicio;

    private static final Long ID_CLIENTE = 100L;
    private static final Long ID_CREADOR = 200L;
    private static final Long ID_ADMIN = 900L;

    private EscrowPayment pagoRetenido;

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
        User cliente = User.builder().idUsuario(ID_CLIENTE).build();
        User creador = User.builder().idUsuario(ID_CREADOR).build();
        CreatorProfile perfil = CreatorProfile.builder().usuario(creador).build();
        Offering servicio = Offering.builder().perfil(perfil).tituloServicio("Offering de prueba").build();
        Order pedido = Order.builder().idPedido(1L).usuarioCliente(cliente).servicio(servicio).build();
        Contract contrato = Contract.builder().idContrato(5L).pedido(pedido).build();

        pagoRetenido = EscrowPayment.builder()
                .idPago(1L)
                .contrato(contrato)
                .idOrdenPaypal("ORDER-123")
                .montoRetenido(new BigDecimal("50.00"))
                .estadoFondos("Retenido")
                .build();

        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contrato));
        given(pagoGarantiaRepository.findByContratoIdContratoParaActualizar(5L))
                .willReturn(Optional.of(pagoRetenido));
        ReflectionTestUtils.setField(pagoServicio, "tasaComision", new BigDecimal("0.10"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComo(String... authorities) {
        List<SimpleGrantedAuthority> roles = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        var auth = new UsernamePasswordAuthenticationToken("usuario-test", "N/A", roles);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void conCapturaCompletada() {
        given(payPalClient.llamarPayPal(anyString(), any(HttpMethod.class), any()))
                .willReturn(json("""
                        {"purchase_units":[{"payments":{"captures":[
                            {"id":"CAPTURE-1","status":"COMPLETED"}
                        ]}}]}"""));
    }

    @Test
    @DisplayName("el cliente puede reembolsarse a si mismo por defecto")
    void reembolsaComoCliente_ok() {
        conCapturaCompletada();
        given(payPalClient.llamarPayPalIdempotente(anyString(), any(HttpMethod.class), any(), anyString()))
                .willReturn(json("""
                        {"id":"REFUND-1","status":"COMPLETED"}"""));

        var respuesta = pagoServicio.cancelarPedidoConFondosRetenidos(1L, ID_CLIENTE, null, "ya no lo necesito");

        assertThat(respuesta.getEstadoFondos()).isEqualTo("Reembolsado");
        assertThat(respuesta.getMensajeError()).isNull();
        verify(pagoGarantiaRepository).save(pagoRetenido);
        verify(transaccionPagoRepository).save(argThatTipo("Reembolso"));
    }

    @Test
    @DisplayName("el cliente no puede liberar los fondos sin reembolsarlos")
    void clienteNoPuedeLiberar_rechaza() {
        assertThatThrownBy(() ->
                pagoServicio.cancelarPedidoConFondosRetenidos(1L, ID_CLIENTE, "LIBERAR", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("administrador");

        verify(pagoGarantiaRepository, never()).save(any());
    }

    @Test
    @DisplayName("un administrador puede liberar los fondos al creador sin pasar por PayPal")
    void adminPuedeLiberar_ok() {
        autenticarComo("ROLE_ADMIN");

        var respuesta = pagoServicio.cancelarPedidoConFondosRetenidos(
                1L, ID_ADMIN, "LIBERAR", "trabajo ya entregado, se libera igual");

        assertThat(respuesta.getEstadoFondos()).isEqualTo("Liberado");
        verify(transaccionPagoRepository).save(argThatTipo("Egreso"));
        verify(transaccionPagoRepository).save(argThatTipo("Comision"));
        verify(payPalClient, never()).llamarPayPalIdempotente(anyString(), any(), any(), anyString());
    }

    @Test
    @DisplayName("un usuario ajeno al pedido no puede cancelarlo")
    void usuarioAjeno_rechaza() {
        assertThatThrownBy(() ->
                pagoServicio.cancelarPedidoConFondosRetenidos(1L, 999L, null, null))
                .isInstanceOf(BusinessRuleException.class);

        verify(pagoGarantiaRepository, never()).save(any());
    }

    @Test
    @DisplayName("un pago que no esta retenido ni con reembolso fallido no se puede cancelar")
    void estadoNoRetenido_rechaza() {
        pagoRetenido.setEstadoFondos("Pendiente");

        assertThatThrownBy(() ->
                pagoServicio.cancelarPedidoConFondosRetenidos(1L, ID_CLIENTE, null, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Pendiente");
    }

    @Test
    @DisplayName("una accionFondos invalida se rechaza")
    void accionInvalida_rechaza() {
        assertThatThrownBy(() ->
                pagoServicio.cancelarPedidoConFondosRetenidos(1L, ID_CLIENTE, "OTRACOSA", null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("si PayPal no tiene ninguna captura completada, el reembolso queda fallido sin llamar a refund")
    void sinCapturaCompletada_marcaReembolsoFallido() {
        given(payPalClient.llamarPayPal(anyString(), any(HttpMethod.class), any()))
                .willReturn(json("""
                        {"purchase_units":[{"payments":{"captures":[]}}]}"""));

        var respuesta = pagoServicio.cancelarPedidoConFondosRetenidos(1L, ID_CLIENTE, null, null);

        assertThat(respuesta.getEstadoFondos()).isEqualTo("ReembolsoFallido");
        assertThat(respuesta.getMensajeError()).contains("captura");
        verify(payPalClient, never()).llamarPayPalIdempotente(anyString(), any(), any(), anyString());
        verify(transaccionPagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("si PayPal rechaza el reembolso, el pago queda ReembolsoFallido y no revienta")
    void fallaRefundPaypal_marcaReembolsoFallido() {
        conCapturaCompletada();
        HttpStatusCodeException error = mock(HttpStatusCodeException.class);
        when(error.getStatusCode()).thenReturn(HttpStatus.UNPROCESSABLE_ENTITY);
        when(error.getResponseBodyAsString()).thenReturn("{\"name\":\"CAPTURE_FULLY_REFUNDED\"}");
        given(payPalClient.llamarPayPalIdempotente(anyString(), any(HttpMethod.class), any(), anyString()))
                .willThrow(error);

        var respuesta = pagoServicio.cancelarPedidoConFondosRetenidos(1L, ID_CLIENTE, "reembolsar", null);

        assertThat(respuesta.getEstadoFondos()).isEqualTo("ReembolsoFallido");
        assertThat(respuesta.getMensajeError()).contains("PayPal");
        verify(transaccionPagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("un reembolso previamente fallido se puede reintentar llamando al mismo metodo")
    void reintentoSobreReembolsoFallido_ok() {
        pagoRetenido.setEstadoFondos("ReembolsoFallido");
        pagoRetenido.setMensajeError("Error de PayPal (422): CAPTURE_FULLY_REFUNDED");
        conCapturaCompletada();
        given(payPalClient.llamarPayPalIdempotente(anyString(), any(HttpMethod.class), any(), anyString()))
                .willReturn(json("""
                        {"id":"REFUND-2","status":"COMPLETED"}"""));

        var respuesta = pagoServicio.cancelarPedidoConFondosRetenidos(1L, ID_CLIENTE, null, null);

        assertThat(respuesta.getEstadoFondos()).isEqualTo("Reembolsado");
        assertThat(respuesta.getMensajeError()).isNull();
        verify(transaccionPagoRepository, times(1)).save(argThatTipo("Reembolso"));
    }

    private PaymentTransaction argThatTipo(String tipo) {
        return org.mockito.ArgumentMatchers.argThat(t -> t != null && tipo.equals(t.getTipoTransaccion()));
    }
}
