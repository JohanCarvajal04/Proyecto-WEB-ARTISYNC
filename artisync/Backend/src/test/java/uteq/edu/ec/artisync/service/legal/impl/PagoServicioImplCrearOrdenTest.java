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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPago;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.legal.PagoGarantiaRepository;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

/**
 * Cubre {@code crearOrdenPayPal} y {@code obtenerEstadoPago}, complementarias
 * a {@link PagoServicioImplWebhookTest} (que solo cubre el webhook).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoServicioImplCrearOrdenTest {

    @Mock private PagoGarantiaRepository pagoGarantiaRepository;
    @Mock private ContratoRepository contratoRepository;
    @Mock private TransaccionPagoRepository transaccionPagoRepository;
    @Mock private RestTemplate restTemplate;

    @InjectMocks
    private PagoServicioImpl pagoServicio;

    private static final Long ID_CLIENTE = 100L;
    private static final Long ID_CREADOR = 200L;
    private static final Long ID_AJENO = 999L;

    private Contrato contratoFirmado;

    @BeforeEach
    void setUp() {
        Usuario cliente = Usuario.builder().idUsuario(ID_CLIENTE).build();
        Usuario creador = Usuario.builder().idUsuario(ID_CREADOR).build();
        PerfilCreador perfil = PerfilCreador.builder().usuario(creador).build();
        Servicio servicio = Servicio.builder().perfil(perfil).build();
        Pedido pedido = Pedido.builder().idPedido(1L).precioPactado(new BigDecimal("40.00"))
                .usuarioCliente(cliente).servicio(servicio).build();
        contratoFirmado = Contrato.builder().idContrato(5L).pedido(pedido)
                .hashFirmaCreador("hash-c").hashFirmaCliente("hash-k").build();

        ReflectionTestUtils.setField(pagoServicio, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("crearOrdenPayPal rechaza si no existe contrato para el pedido")
    void crearOrden_rechazaSinContrato() {
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pagoServicio.crearOrdenPayPal(1L, ID_CLIENTE, null))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("crearOrdenPayPal rechaza si el usuario autenticado no es el cliente del pedido")
    void crearOrden_rechazaClienteAjeno() {
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contratoFirmado));

        assertThatThrownBy(() -> pagoServicio.crearOrdenPayPal(1L, ID_AJENO, null))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("crearOrdenPayPal rechaza si el contrato no esta firmado por ambas partes")
    void crearOrden_rechazaContratoSinFirmar() {
        Contrato sinFirmar = Contrato.builder().idContrato(5L).pedido(contratoFirmado.getPedido()).build();
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(sinFirmar));

        assertThatThrownBy(() -> pagoServicio.crearOrdenPayPal(1L, ID_CLIENTE, null))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("crearOrdenPayPal rechaza si ya existe un pago en un estado distinto de pendiente")
    void crearOrden_rechazaPagoYaConfirmado() {
        PagoGarantia pagoRetenido = PagoGarantia.builder().idPago(1L).estadoFondos("Retenido").build();
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contratoFirmado));
        given(pagoGarantiaRepository.findByContratoIdContrato(5L)).willReturn(Optional.of(pagoRetenido));

        assertThatThrownBy(() -> pagoServicio.crearOrdenPayPal(1L, ID_CLIENTE, null))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("crearOrdenPayPal crea la orden y devuelve el approvalUrl cuando todo es valido")
    void crearOrden_creaOrdenExitosamente() {
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contratoFirmado));
        given(pagoGarantiaRepository.findByContratoIdContrato(5L)).willReturn(Optional.empty());
        given(pagoGarantiaRepository.save(any(PagoGarantia.class))).willAnswer(inv -> {
            PagoGarantia p = inv.getArgument(0);
            p.setIdPago(10L);
            return p;
        });
        conRespuestasPayPal("""
                {"id":"ORDER-999","links":[{"rel":"approve","href":"https://paypal.com/approve/999"}]}""");

        RespuestaPago respuesta = pagoServicio.crearOrdenPayPal(1L, ID_CLIENTE, null);

        assertThat(respuesta.getIdOrdenPaypal()).isEqualTo("ORDER-999");
        assertThat(respuesta.getApprovalUrl()).isEqualTo("https://paypal.com/approve/999");
        assertThat(respuesta.getMontoRetenido()).isEqualByComparingTo("40.00");
    }

    @Test
    @DisplayName("crearOrdenPayPal usa el monto indicado en vez del precio pactado cuando se provee")
    void crearOrden_usaMontoIndicado() {
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contratoFirmado));
        given(pagoGarantiaRepository.findByContratoIdContrato(5L)).willReturn(Optional.empty());
        given(pagoGarantiaRepository.save(any(PagoGarantia.class))).willAnswer(inv -> inv.getArgument(0));
        conRespuestasPayPal("""
                {"id":"ORDER-999","links":[]}""");

        RespuestaPago respuesta = pagoServicio.crearOrdenPayPal(1L, ID_CLIENTE, new BigDecimal("99.00"));

        assertThat(respuesta.getMontoRetenido()).isEqualByComparingTo("99.00");
        assertThat(respuesta.getApprovalUrl()).isEmpty();
    }

    @Test
    @DisplayName("crearOrdenPayPal envuelve un fallo de comunicacion con PayPal como regla de negocio")
    void crearOrden_envuelveErrorDeComunicacion() {
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contratoFirmado));
        given(pagoGarantiaRepository.findByContratoIdContrato(5L)).willReturn(Optional.empty());
        given(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(Map.class)))
                .willThrow(new RuntimeException("timeout"));

        assertThatThrownBy(() -> pagoServicio.crearOrdenPayPal(1L, ID_CLIENTE, null))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    // ---------- obtenerEstadoPago ----------

    @Test
    @DisplayName("obtenerEstadoPago devuelve el pago existente al cliente")
    void obtenerEstadoPago_devuelvePagoAlCliente() {
        PagoGarantia pago = PagoGarantia.builder().idPago(1L).idOrdenPaypal("ORDER-1")
                .montoRetenido(new BigDecimal("40.00")).estadoFondos("Retenido").build();
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contratoFirmado));
        given(pagoGarantiaRepository.findByContratoIdContrato(5L)).willReturn(Optional.of(pago));

        RespuestaPago respuesta = pagoServicio.obtenerEstadoPago(1L, ID_CLIENTE);

        assertThat(respuesta.getEstadoFondos()).isEqualTo("Retenido");
    }

    @Test
    @DisplayName("obtenerEstadoPago devuelve el pago existente al creador")
    void obtenerEstadoPago_devuelvePagoAlCreador() {
        PagoGarantia pago = PagoGarantia.builder().idPago(1L).idOrdenPaypal("ORDER-1")
                .montoRetenido(new BigDecimal("40.00")).estadoFondos("Retenido").build();
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contratoFirmado));
        given(pagoGarantiaRepository.findByContratoIdContrato(5L)).willReturn(Optional.of(pago));

        RespuestaPago respuesta = pagoServicio.obtenerEstadoPago(1L, ID_CREADOR);

        assertThat(respuesta.getEstadoFondos()).isEqualTo("Retenido");
    }

    @Test
    @DisplayName("obtenerEstadoPago rechaza a un usuario que no es parte del pedido")
    void obtenerEstadoPago_rechazaUsuarioAjeno() {
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contratoFirmado));

        assertThatThrownBy(() -> pagoServicio.obtenerEstadoPago(1L, ID_AJENO))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("obtenerEstadoPago lanza recurso no encontrado si no existe contrato")
    void obtenerEstadoPago_sinContrato() {
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pagoServicio.obtenerEstadoPago(1L, ID_CLIENTE))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerEstadoPago lanza recurso no encontrado si no existe pago registrado")
    void obtenerEstadoPago_sinPago() {
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contratoFirmado));
        given(pagoGarantiaRepository.findByContratoIdContrato(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pagoServicio.obtenerEstadoPago(1L, ID_CLIENTE))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    private void conRespuestasPayPal(String cuerpoOrden) {
        given(restTemplate.exchange(contains("/v1/oauth2/token"), any(HttpMethod.class),
                any(HttpEntity.class), eq(Map.class)))
                .willReturn(ResponseEntity.ok(Map.of("access_token", "token-de-prueba")));
        given(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
                .willReturn(ResponseEntity.ok(cuerpoOrden));
    }
}
