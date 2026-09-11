package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import uteq.edu.ec.artisync.dto.respuesta.legal.DeliverableResponse;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.legal.FinalDeliverable;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.legal.FinalDeliverableRepository;
import uteq.edu.ec.artisync.repository.legal.EscrowPaymentRepository;
import uteq.edu.ec.artisync.repository.legal.PaymentTransactionRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.service.legal.IDeliverableService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliverableServiceImplTest {

    private static final Long ID_PEDIDO = 7L;
    private static final Long ID_CREADOR = 1L;
    private static final Long ID_CLIENTE = 2L;
    private static final Long ID_TERCERO = 99L;

    @Mock private FinalDeliverableRepository entregableRepository;
    @Mock private OrderRepository pedidoRepository;
    @Mock private EscrowPaymentRepository pagoGarantiaRepository;
    @Mock private ContractRepository contratoRepository;
    @Mock private PaymentTransactionRepository transaccionPagoRepository;
    @Mock private DocumentStorage almacenamiento;
    @Mock private uteq.edu.ec.artisync.service.comunicacion.ChatService chatService;
    @Mock private uteq.edu.ec.artisync.service.comunicacion.NotificationService notificacionService;

    @InjectMocks private DeliverableServiceImpl servicio;

    private Order pedido;

    @BeforeEach
    void setUp() {
        User creador = new User();
        creador.setIdUsuario(ID_CREADOR);
        CreatorProfile perfil = new CreatorProfile();
        perfil.setUsuario(creador);
        Offering servicioCatalogo = new Offering();
        servicioCatalogo.setPerfil(perfil);

        User cliente = new User();
        cliente.setIdUsuario(ID_CLIENTE);

        pedido = new Order();
        pedido.setIdPedido(ID_PEDIDO);
        pedido.setServicio(servicioCatalogo);
        pedido.setUsuarioCliente(cliente);

        // @Value no lo rellena @InjectMocks (no es parte del constructor de
        // Lombok al no ser final); mismo patron que
        // SolicitudRetiroServicioImplTest con montoMinimo.
        ReflectionTestUtils.setField(servicio, "tasaComision", new java.math.BigDecimal("0.10"));
    }

    private MockMultipartFile imagen(String nombre) {
        return new MockMultipartFile(nombre, nombre + ".png", "image/png", "contenido".getBytes());
    }

    private FinalDeliverable entregableGuardado(String marcaAgua, String limpia, boolean liberado) {
        return FinalDeliverable.builder()
                .idEntregable(1L)
                .pedido(pedido)
                .urlVersionMarcaAgua(marcaAgua)
                .urlVersionLimpia(limpia)
                .estaLiberado(liberado)
                .build();
    }

    // ── Subida ───────────────────────────────────────────────────────────────

    @Test
    void subirEntregable_guardaAmbasVersionesBajoElPrefijoDeEntregables() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO)).thenReturn(Optional.empty());
        when(almacenamiento.guardar(any(), eq("entregables")))
                .thenReturn("entregables/marca.png", "entregables/limpia.png");
        when(entregableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        DeliverableResponse respuesta = servicio.subirEntregable(
                ID_PEDIDO, ID_CREADOR, imagen("marca"), imagen("limpia"));

        verify(almacenamiento, times(2)).guardar(any(), eq("entregables"));
        assertThat(respuesta.getIdPedido()).isEqualTo(ID_PEDIDO);
    }

    @Test
    void subirEntregable_usuarioQueNoEsElCreador_esRechazado() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));

        assertThrows(BusinessRuleException.class, () -> servicio.subirEntregable(
                ID_PEDIDO, ID_TERCERO, imagen("marca"), imagen("limpia")));

        verify(almacenamiento, never()).guardar(any(), anyString());
    }

    @Test
    void subirEntregable_formatoNoPermitido_seRechazaAntesDeTocarLaBase() {
        MockMultipartFile ejecutable = new MockMultipartFile(
                "versionLimpia", "virus.exe", "application/x-msdownload", "MZ".getBytes());

        assertThrows(BusinessRuleException.class, () -> servicio.subirEntregable(
                ID_PEDIDO, ID_CREADOR, imagen("marca"), ejecutable));

        verifyNoInteractions(pedidoRepository, almacenamiento);
    }

    /** Resubir no debe dejar los archivos anteriores facturándose en Azure. */
    @Test
    void subirEntregable_sobreUnoExistente_borraLasReferenciasAnteriores() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/vieja-marca.png",
                        "entregables/vieja-limpia.png", false)));
        when(almacenamiento.guardar(any(), eq("entregables")))
                .thenReturn("entregables/nueva-marca.png", "entregables/nueva-limpia.png");
        when(entregableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        servicio.subirEntregable(ID_PEDIDO, ID_CREADOR, imagen("marca"), imagen("limpia"));

        verify(almacenamiento).eliminar("entregables/vieja-marca.png");
        verify(almacenamiento).eliminar("entregables/vieja-limpia.png");
    }

    @Test
    void subirEntregable_siFallaBorrarLoViejo_laSubidaIgualSeCompleta() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/vieja.png", "entregables/vieja2.png", false)));
        when(almacenamiento.guardar(any(), eq("entregables")))
                .thenReturn("entregables/nueva.png", "entregables/nueva2.png");
        when(entregableRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());
        doThrow(new BusinessRuleException("Azure caido")).when(almacenamiento).eliminar(anyString());

        DeliverableResponse respuesta = servicio.subirEntregable(
                ID_PEDIDO, ID_CREADOR, imagen("marca"), imagen("limpia"));

        assertThat(respuesta).isNotNull();
    }

    // ── Descarga de la version limpia ────────────────────────────────────────

    @Test
    void descargarVersionLimpia_sinPagoLiberado_esRechazada() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", false)));

        assertThrows(BusinessRuleException.class,
                () -> servicio.descargarVersionLimpia(ID_PEDIDO, ID_CLIENTE));

        verify(almacenamiento, never()).leer(anyString());
    }

    @Test
    void descargarVersionLimpia_liberada_devuelveBytesRealesYSuContentType() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.pdf", true)));
        when(almacenamiento.leer("entregables/l.pdf")).thenReturn("%PDF".getBytes());

        IDeliverableService.ArchivoDescargado archivo =
                servicio.descargarVersionLimpia(ID_PEDIDO, ID_CLIENTE);

        assertThat(archivo.contenido()).isEqualTo("%PDF".getBytes());
        assertThat(archivo.contentType()).isEqualTo("application/pdf");
        assertThat(archivo.nombreSugerido()).isEqualTo("entregable-pedido-7.pdf");
    }

    @Test
    void descargarVersionLimpia_usuarioQueNoEsElCliente_esRechazado() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));

        assertThrows(BusinessRuleException.class,
                () -> servicio.descargarVersionLimpia(ID_PEDIDO, ID_TERCERO));
    }

    // ── Descarga de la version con marca de agua ─────────────────────────────

    @Test
    void descargarMarcaAgua_clienteYCreadorPuedenVerlaSinPagoLiberado() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", false)));
        when(almacenamiento.leer("entregables/m.png")).thenReturn("png".getBytes());

        assertThat(servicio.descargarVersionMarcaAgua(ID_PEDIDO, ID_CLIENTE).contenido()).isNotEmpty();
        assertThat(servicio.descargarVersionMarcaAgua(ID_PEDIDO, ID_CREADOR).contenido()).isNotEmpty();
    }

    @Test
    void descargarMarcaAgua_terceroSinRelacionConElPedido_esRechazado() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", false)));

        assertThrows(BusinessRuleException.class,
                () -> servicio.descargarVersionMarcaAgua(ID_PEDIDO, ID_TERCERO));

        verify(almacenamiento, never()).leer(anyString());
    }

    @Test
    void descargarMarcaAgua_entregableSinArchivo_reportaRecursoNoEncontrado() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado(null, "entregables/l.png", false)));

        assertThrows(ResourceNotFoundException.class,
                () -> servicio.descargarVersionMarcaAgua(ID_PEDIDO, ID_CLIENTE));
    }

    // ── Respuesta ────────────────────────────────────────────────────────────

    /** Con Azure el archivo viaja directo desde el blob, sin pasar por el backend. */
    @Test
    void obtenerEntregable_conProveedorQueFirmaUrls_devuelveElSasYNoLaRutaDelBackend() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", true)));
        when(almacenamiento.urlTemporal("entregables/m.png"))
                .thenReturn(Optional.of("https://cuenta.blob.core.windows.net/c/entregables/m.png?sig=x"));
        when(almacenamiento.urlTemporal("entregables/l.png"))
                .thenReturn(Optional.of("https://cuenta.blob.core.windows.net/c/entregables/l.png?sig=y"));

        DeliverableResponse respuesta = servicio.obtenerEntregable(ID_PEDIDO, ID_CLIENTE);

        assertThat(respuesta.getUrlVersionMarcaAgua()).startsWith("https://").contains("sig=");
        assertThat(respuesta.getUrlVersionLimpia()).startsWith("https://").contains("sig=");
    }

    @Test
    void obtenerEntregable_conAlmacenamientoLocal_caeALaRutaDelBackend() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", true)));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        DeliverableResponse respuesta = servicio.obtenerEntregable(ID_PEDIDO, ID_CLIENTE);

        assertThat(respuesta.getUrlVersionMarcaAgua())
                .isEqualTo("/api/v1/pedidos/7/entregable/descargar/marca-agua");
        assertThat(respuesta.getUrlVersionLimpia())
                .isEqualTo("/api/v1/pedidos/7/entregable/descargar");
    }

    /** El cliente no debe recibir la version limpia mientras no libere el pago. */
    @Test
    void obtenerEntregable_clienteSinLiberar_noRecibeLaVersionLimpia() {
        when(entregableRepository.findByPedidoIdPedido(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("entregables/m.png", "entregables/l.png", false)));
        when(almacenamiento.urlTemporal("entregables/m.png")).thenReturn(Optional.empty());

        DeliverableResponse respuesta = servicio.obtenerEntregable(ID_PEDIDO, ID_CLIENTE);

        assertThat(respuesta.getUrlVersionLimpia()).isNull();
        assertThat(respuesta.getUrlVersionMarcaAgua()).isNotNull();
    }

    // ── Aprobar Entrega ──────────────────────────────────────────────────────
    @Test
    void aprobarEntrega_ok() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedidoParaActualizar(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("m", "l", false)));

        uteq.edu.ec.artisync.entity.legal.Contract contrato = new uteq.edu.ec.artisync.entity.legal.Contract();
        contrato.setIdContrato(1L);
        when(contratoRepository.findByPedidoIdPedido(ID_PEDIDO)).thenReturn(Optional.of(contrato));

        uteq.edu.ec.artisync.entity.legal.EscrowPayment pago = new uteq.edu.ec.artisync.entity.legal.EscrowPayment();
        pago.setMontoRetenido(new java.math.BigDecimal("100.00"));
        when(pagoGarantiaRepository.findByContratoIdContrato(1L)).thenReturn(Optional.of(pago));

        servicio.aprobarEntrega(ID_PEDIDO, ID_CLIENTE);

        verify(transaccionPagoRepository, times(2)).save(any());
        verify(entregableRepository).save(any());
    }

    /** La tasa ya no es un literal 0.10: confirma que usa la propiedad configurada. */
    @Test
    void aprobarEntrega_usaLaTasaDeComisionConfigurada() {
        ReflectionTestUtils.setField(servicio, "tasaComision", new java.math.BigDecimal("0.20"));

        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedidoParaActualizar(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("m", "l", false)));

        uteq.edu.ec.artisync.entity.legal.Contract contrato = new uteq.edu.ec.artisync.entity.legal.Contract();
        contrato.setIdContrato(1L);
        when(contratoRepository.findByPedidoIdPedido(ID_PEDIDO)).thenReturn(Optional.of(contrato));

        uteq.edu.ec.artisync.entity.legal.EscrowPayment pago = new uteq.edu.ec.artisync.entity.legal.EscrowPayment();
        pago.setMontoRetenido(new java.math.BigDecimal("100.00"));
        when(pagoGarantiaRepository.findByContratoIdContrato(1L)).thenReturn(Optional.of(pago));

        var captor = org.mockito.ArgumentCaptor.forClass(
                uteq.edu.ec.artisync.entity.legal.PaymentTransaction.class);

        servicio.aprobarEntrega(ID_PEDIDO, ID_CLIENTE);

        verify(transaccionPagoRepository, times(2)).save(captor.capture());
        var porTipo = captor.getAllValues().stream()
                .collect(java.util.stream.Collectors.toMap(
                        uteq.edu.ec.artisync.entity.legal.PaymentTransaction::getTipoTransaccion,
                        uteq.edu.ec.artisync.entity.legal.PaymentTransaction::getMonto));
        assertThat(porTipo.get("Comision")).isEqualByComparingTo("20.00");
        assertThat(porTipo.get("Egreso")).isEqualByComparingTo("80.00");
    }

    /**
     * REQ-NF-019: sin este guard, un pedido ya cancelado-y-reembolsado (o
     * liberado) por PaymentServiceImpl.cancelarPedidoConFondosRetenidos podía
     * aprobarse aquí después y pagar al creador una segunda vez.
     */
    @Test
    void aprobarEntrega_pagoNoRetenido_error() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedidoParaActualizar(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("m", "l", false)));

        uteq.edu.ec.artisync.entity.legal.Contract contrato = new uteq.edu.ec.artisync.entity.legal.Contract();
        contrato.setIdContrato(1L);
        when(contratoRepository.findByPedidoIdPedido(ID_PEDIDO)).thenReturn(Optional.of(contrato));

        uteq.edu.ec.artisync.entity.legal.EscrowPayment pago = new uteq.edu.ec.artisync.entity.legal.EscrowPayment();
        pago.setMontoRetenido(new java.math.BigDecimal("100.00"));
        pago.setEstadoFondos("Reembolsado");
        when(pagoGarantiaRepository.findByContratoIdContrato(1L)).thenReturn(Optional.of(pago));

        assertThrows(BusinessRuleException.class, () -> servicio.aprobarEntrega(ID_PEDIDO, ID_CLIENTE));

        verify(transaccionPagoRepository, never()).save(any());
        verify(entregableRepository, never()).save(any());
    }

    @Test
    void aprobarEntrega_noEsCliente_error() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));

        assertThrows(BusinessRuleException.class, () -> servicio.aprobarEntrega(ID_PEDIDO, ID_TERCERO));
    }

    @Test
    void aprobarEntrega_yaLiberado_error() {
        when(pedidoRepository.findById(ID_PEDIDO)).thenReturn(Optional.of(pedido));
        when(entregableRepository.findByPedidoIdPedidoParaActualizar(ID_PEDIDO))
                .thenReturn(Optional.of(entregableGuardado("m", "l", true)));

        assertThrows(BusinessRuleException.class, () -> servicio.aprobarEntrega(ID_PEDIDO, ID_CLIENTE));
    }
}
