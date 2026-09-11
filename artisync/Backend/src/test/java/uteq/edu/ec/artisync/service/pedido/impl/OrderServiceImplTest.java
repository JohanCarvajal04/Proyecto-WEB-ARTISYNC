package uteq.edu.ec.artisync.service.pedido.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.AnswerBriefingRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.AdvanceStageRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateOrderRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateTermsProposalRequest;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.StatusHistoryResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderSummaryResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.TermsProposalResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderTrackingResponse;
import uteq.edu.ec.artisync.entity.catalogo.Workflow;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.comunicacion.SentBriefing;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingTemplate;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingQuestion;
import uteq.edu.ec.artisync.entity.pedido.WorkflowStage;
import uteq.edu.ec.artisync.entity.pedido.WorkflowStageConfig;
import uteq.edu.ec.artisync.entity.pedido.OrderStatusHistory;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.pedido.OrderTermsProposal;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.repository.comunicacion.SentBriefingRepository;
import uteq.edu.ec.artisync.repository.comunicacion.BriefingAnswerRepository;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.legal.FinalDeliverableRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderTermsProposalRepository;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;
import uteq.edu.ec.artisync.service.legal.IContractService;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.WorkflowRepository;
import uteq.edu.ec.artisync.repository.catalogo.OfferingRepository;
import uteq.edu.ec.artisync.repository.pedido.WorkflowStageRepository;
import uteq.edu.ec.artisync.repository.pedido.WorkflowStageConfigRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderStatusHistoryRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.perfil.IVerificationService;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.IExportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Pruebas unitarias de {@link OrderServiceImpl} complementarias a
 * {@link PedidoServicioImplFlujoTest} (que solo cubre la resolución del flujo
 * por categoría, RF-19): aquí se cubren el resto de operaciones — obtener,
 * listar, avanzar etapa, historial y seguimiento — junto con el control IDOR
 * de {@code obtenerPedidoPorId} (OBS-08).
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository pedidoRepository;
    @Mock private OfferingRepository servicioRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private WorkflowRepository flujoTrabajoRepository;
    @Mock private WorkflowStageConfigRepository flujoEtapaConfigRepository;
    @Mock private OrderStatusHistoryRepository historialRepository;
    @Mock private WorkflowStageRepository etapaFlujoRepository;
    @Mock private ContractRepository contratoRepository;
    @Mock private FinalDeliverableRepository entregableFinalRepository;
    @Mock private OrderTermsProposalRepository propuestaTerminosPedidoRepository;
    @Mock private NotificationService notificacionService;
    @Mock private ChatService chatService;
    @Mock private IExportService servicioExportacion;
    @Mock private IVerificationService verificacionServicio;
    @Mock private IContractService contratoServicio;
    @Mock private SentBriefingRepository briefingEnviadoRepository;
    @Mock private BriefingAnswerRepository briefingRespuestaRepository;

    @InjectMocks
    private OrderServiceImpl pedidoServicio;

    private User cliente;
    private User creador;
    private Workflow flujo;
    private Offering servicio;
    private Order pedido;
    private WorkflowStage etapaInicial;
    private WorkflowStage etapaSiguiente;

    @BeforeEach
    void setUp() {
        cliente = User.builder().idUsuario(1L).nombres("Cliente").apellidos("Uno").build();
        creador = User.builder().idUsuario(2L).nombres("Creador").apellidos("Uno").build();
        flujo = Workflow.builder().idFlujo(1L).nombreFlujo("Flujo estandar").build();
        CreatorProfile perfil = CreatorProfile.builder().idPerfil(1L).usuario(creador).build();
        servicio = Offering.builder().idServicio(1L).perfil(perfil)
                .tituloServicio("Ilustracion").precioBase(new BigDecimal("20.00")).flujo(flujo).build();
        pedido = Order.builder().idPedido(10L).usuarioCliente(cliente).servicio(servicio)
                .flujo(flujo).precioPactado(new BigDecimal("20.00")).build();
        etapaInicial = WorkflowStage.builder().idEtapa(1L).nombreEtapa("Inicio").build();
        etapaSiguiente = WorkflowStage.builder().idEtapa(2L).nombreEtapa("Revision").build();

        // Por defecto el cliente ya tiene su identidad verificada: la mayoría
        // de estos tests no ejercitan el gating de REQ-F-006 ampliado.
        lenient().when(verificacionServicio.estaIdentidadVerificada(anyLong())).thenReturn(true);
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    // ---------- crearPedido ----------

    @Test
    @DisplayName("crearPedido rechaza si el cliente es el mismo creador del servicio")
    void crearPedido_rechazaAutoPedido() {
        CreateOrderRequest peticion = CreateOrderRequest.builder().idServicio(1L).build();
        given(usuarioRepository.findById(2L)).willReturn(Optional.of(creador));
        given(servicioRepository.findById(1L)).willReturn(Optional.of(servicio));

        assertThatThrownBy(() -> pedidoServicio.crearPedido(2L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("crearPedido rechaza si el flujo no tiene etapas configuradas")
    void crearPedido_rechazaFlujoSinEtapas() {
        CreateOrderRequest peticion = CreateOrderRequest.builder().idServicio(1L).build();
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(servicioRepository.findById(1L)).willReturn(Optional.of(servicio));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        assertThatThrownBy(() -> pedidoServicio.crearPedido(1L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("crearPedido usa el precio base cuando no se ofrece un precio")
    void crearPedido_usaPrecioBase() {
        CreateOrderRequest peticion = CreateOrderRequest.builder().idServicio(1L).build();
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();

        given(usuarioRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(servicioRepository.findById(1L)).willReturn(Optional.of(servicio));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(config));
        given(pedidoRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(any())).willReturn(List.of());

        OrderResponse respuesta = pedidoServicio.crearPedido(1L, peticion);

        assertThat(respuesta.getPrecioPactado()).isEqualByComparingTo("20.00");
        // La sala se abre desde la creación, no al firmar: así pueden
        // negociar por chat antes de comprometerse con el contrato.
        verify(chatService).crearSala(any(Order.class));
    }

    @Test
    @DisplayName("crearPedido lanza recurso no encontrado si el cliente no existe")
    void crearPedido_clienteInexistente() {
        given(usuarioRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.crearPedido(1L, CreateOrderRequest.builder().idServicio(1L).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crearPedido rechaza si el cliente no tiene la identidad verificada")
    void crearPedido_identidadNoVerificada_lanzaExcepcionReglaNegocio() {
        given(usuarioRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(verificacionServicio.estaIdentidadVerificada(1L)).willReturn(false);

        assertThatThrownBy(() -> pedidoServicio.crearPedido(1L, CreateOrderRequest.builder().idServicio(1L).build()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("verificar tu identidad");
        verifyNoInteractions(servicioRepository);
    }

    // ---------- crearPedido: cuestionario (briefing) por servicio, REQ-F-016 ampliado ----------

    private BriefingTemplate plantillaBriefingDeDosPreguntas() {
        BriefingQuestion p1 = BriefingQuestion.builder().idPregunta(101L).textoPregunta("¿Colores preferidos?").numeroOrden(1).build();
        BriefingQuestion p2 = BriefingQuestion.builder().idPregunta(102L).textoPregunta("¿Referencias?").numeroOrden(2).build();
        return BriefingTemplate.builder().idBriefingPlantilla(50L).nombrePlantilla("Briefing Logo")
                .preguntas(List.of(p1, p2)).build();
    }

    @Test
    @DisplayName("crearPedido — servicio con cuestionario y todas las respuestas crea el pedido y registra el briefing completado")
    void crearPedido_conCuestionarioCompleto_registraBriefing() {
        servicio.setBriefingPlantilla(plantillaBriefingDeDosPreguntas());
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();

        CreateOrderRequest peticion = CreateOrderRequest.builder().idServicio(1L)
                .respuestasBriefing(List.of(
                        AnswerBriefingRequest.RespuestaItem.builder().idPregunta(101L).textoRespuesta("Azul y blanco").build(),
                        AnswerBriefingRequest.RespuestaItem.builder().idPregunta(102L).textoRespuesta("Ninguna en particular").build()
                ))
                .build();

        given(usuarioRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(servicioRepository.findById(1L)).willReturn(Optional.of(servicio));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(config));
        given(pedidoRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(any())).willReturn(List.of());
        given(briefingEnviadoRepository.save(any(SentBriefing.class))).willAnswer(inv -> {
            SentBriefing be = inv.getArgument(0);
            be.setIdBriefingEnviado(500L);
            return be;
        });

        OrderResponse respuesta = pedidoServicio.crearPedido(1L, peticion);

        assertThat(respuesta).isNotNull();
        verify(briefingEnviadoRepository).save(argThat(be -> Boolean.TRUE.equals(be.getCompletado())));
        verify(briefingRespuestaRepository, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    @DisplayName("crearPedido — servicio con cuestionario y respuestas incompletas rechaza y no crea el pedido")
    void crearPedido_conCuestionarioIncompleto_rechazaYNoPersisteNada() {
        servicio.setBriefingPlantilla(plantillaBriefingDeDosPreguntas());
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();

        CreateOrderRequest peticion = CreateOrderRequest.builder().idServicio(1L)
                .respuestasBriefing(List.of(
                        AnswerBriefingRequest.RespuestaItem.builder().idPregunta(101L).textoRespuesta("Azul y blanco").build()
                        // falta la respuesta a la pregunta 102
                ))
                .build();

        given(usuarioRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(servicioRepository.findById(1L)).willReturn(Optional.of(servicio));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(config));

        assertThatThrownBy(() -> pedidoServicio.crearPedido(1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Referencias");
        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(briefingEnviadoRepository, briefingRespuestaRepository);
    }

    @Test
    @DisplayName("crearPedido — servicio con cuestionario y sin respuestas rechaza y no crea el pedido")
    void crearPedido_conCuestionarioSinRespuestas_rechaza() {
        servicio.setBriefingPlantilla(plantillaBriefingDeDosPreguntas());
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();
        CreateOrderRequest peticion = CreateOrderRequest.builder().idServicio(1L).build();

        given(usuarioRepository.findById(1L)).willReturn(Optional.of(cliente));
        given(servicioRepository.findById(1L)).willReturn(Optional.of(servicio));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(config));

        assertThatThrownBy(() -> pedidoServicio.crearPedido(1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cuestionario");
        verify(pedidoRepository, never()).save(any());
    }

    // ---------- proponerTerminos / aceptarPropuestaTerminos / rechazarPropuestaTerminos ----------

    @Test
    @DisplayName("proponerTerminos rechaza si no llega ningun campo")
    void proponerTerminos_rechazaPeticionVacia() {
        assertThatThrownBy(() -> pedidoServicio.proponerTerminos(
                10L, 1L, CreateTermsProposalRequest.builder().build()))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("proponerTerminos rechaza a un usuario que no es cliente ni creador del pedido")
    void proponerTerminos_rechazaUsuarioAjeno() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        CreateTermsProposalRequest peticion =
                CreateTermsProposalRequest.builder().precioPropuesto(new BigDecimal("35.00")).build();

        assertThatThrownBy(() -> pedidoServicio.proponerTerminos(10L, 999L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("proponerTerminos rechaza si ya existe una propuesta pendiente")
    void proponerTerminos_rechazaSiYaHayPropuestaPendiente() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(10L)).willReturn(Optional.empty());
        given(propuestaTerminosPedidoRepository.findByPedidoIdPedidoAndEstado(10L, OrderTermsProposal.PENDIENTE))
                .willReturn(Optional.of(OrderTermsProposal.builder().idPropuesta(1L).build()));
        CreateTermsProposalRequest peticion =
                CreateTermsProposalRequest.builder().precioPropuesto(new BigDecimal("35.00")).build();

        assertThatThrownBy(() -> pedidoServicio.proponerTerminos(10L, 1L, peticion))
                .isInstanceOf(BusinessRuleException.class);

        verify(propuestaTerminosPedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("proponerTerminos guarda la propuesta y notifica al creador cuando propone el cliente")
    void proponerTerminos_clientePuedeProponerYNotificaCreador() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(10L)).willReturn(Optional.empty());
        given(propuestaTerminosPedidoRepository.findByPedidoIdPedidoAndEstado(10L, OrderTermsProposal.PENDIENTE))
                .willReturn(Optional.empty());
        given(propuestaTerminosPedidoRepository.save(any(OrderTermsProposal.class)))
                .willAnswer(inv -> inv.getArgument(0));
        CreateTermsProposalRequest peticion =
                CreateTermsProposalRequest.builder().precioPropuesto(new BigDecimal("35.00")).build();

        TermsProposalResponse respuesta = pedidoServicio.proponerTerminos(10L, 1L, peticion);

        assertThat(respuesta.getPrecioPropuesto()).isEqualByComparingTo("35.00");
        assertThat(respuesta.getEstado()).isEqualTo(OrderTermsProposal.PENDIENTE);
        verify(notificacionService).notificar(org.mockito.ArgumentMatchers.eq(creador), anyString(), anyString());
    }

    @Test
    @DisplayName("proponerTerminos rechaza si el contrato ya tiene alguna firma")
    void proponerTerminos_rechazaConContratoFirmado() {
        Contract contrato = Contract.builder().idContrato(5L).pedido(pedido).hashFirmaCliente("hash").build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(10L)).willReturn(Optional.of(contrato));
        CreateTermsProposalRequest peticion =
                CreateTermsProposalRequest.builder().precioPropuesto(new BigDecimal("35.00")).build();

        assertThatThrownBy(() -> pedidoServicio.proponerTerminos(10L, 1L, peticion))
                .isInstanceOf(BusinessRuleException.class);

        verify(propuestaTerminosPedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("aceptarPropuestaTerminos aplica el cambio y genera el contrato si aun no existia")
    void aceptarPropuestaTerminos_aplicaCambioYGeneraContrato() {
        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(OrderTermsProposal.PENDIENTE).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));
        given(contratoRepository.findByPedidoIdPedido(10L)).willReturn(Optional.empty());
        given(pedidoRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(propuestaTerminosPedidoRepository.save(any(OrderTermsProposal.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());
        given(contratoServicio.generarContrato(10L, 2L)).willReturn(ContractResponse.builder().idContrato(99L).build());

        // El creador (idUsuario=2) acepta la propuesta creada por el cliente (idUsuario=1).
        OrderResponse respuesta = pedidoServicio.aceptarPropuestaTerminos(10L, 7L, 2L);

        assertThat(respuesta.getPrecioPactado()).isEqualByComparingTo("35.00");
        assertThat(propuesta.getEstado()).isEqualTo(OrderTermsProposal.ACEPTADA);
        verify(contratoServicio).generarContrato(10L, 2L);
        verify(notificacionService).notificar(org.mockito.ArgumentMatchers.eq(cliente), anyString(), anyString());
    }

    @Test
    @DisplayName("aceptarPropuestaTerminos no regenera el contrato si ya existia")
    void aceptarPropuestaTerminos_noRegeneraContratoExistente() {
        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(OrderTermsProposal.PENDIENTE).build();
        Contract contratoExistente = Contract.builder().idContrato(3L).pedido(pedido).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));
        given(contratoRepository.findByPedidoIdPedido(10L)).willReturn(Optional.of(contratoExistente));
        given(pedidoRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));
        given(propuestaTerminosPedidoRepository.save(any(OrderTermsProposal.class)))
                .willAnswer(inv -> inv.getArgument(0));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        pedidoServicio.aceptarPropuestaTerminos(10L, 7L, 2L);

        verify(contratoServicio, never()).generarContrato(any(), any());
    }

    @Test
    @DisplayName("aceptarPropuestaTerminos rechaza la auto-aceptacion")
    void aceptarPropuestaTerminos_rechazaAutoAceptacion() {
        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(OrderTermsProposal.PENDIENTE).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));

        assertThatThrownBy(() -> pedidoServicio.aceptarPropuestaTerminos(10L, 7L, 1L))
                .isInstanceOf(BusinessRuleException.class);

        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(contratoServicio);
    }

    @Test
    @DisplayName("rechazarPropuestaTerminos marca la propuesta rechazada sin tocar el pedido")
    void rechazarPropuestaTerminos_marcaRechazadaSinTocarPedido() {
        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(OrderTermsProposal.PENDIENTE).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));
        given(propuestaTerminosPedidoRepository.save(any(OrderTermsProposal.class)))
                .willAnswer(inv -> inv.getArgument(0));

        TermsProposalResponse respuesta = pedidoServicio.rechazarPropuestaTerminos(10L, 7L, 2L);

        assertThat(respuesta.getEstado()).isEqualTo(OrderTermsProposal.RECHAZADA);
        verify(pedidoRepository, never()).save(any());
        verify(notificacionService).notificar(org.mockito.ArgumentMatchers.eq(cliente), anyString(), anyString());
    }

    @Test
    @DisplayName("rechazarPropuestaTerminos rechaza que quien propuso rechace su propia propuesta")
    void rechazarPropuestaTerminos_rechazaAutoRechazo() {
        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(OrderTermsProposal.PENDIENTE).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));

        assertThatThrownBy(() -> pedidoServicio.rechazarPropuestaTerminos(10L, 7L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("propia propuesta");
        verify(propuestaTerminosPedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelarPropuestaTerminos marca la propuesta cancelada cuando la cancela quien la propuso")
    void cancelarPropuestaTerminos_marcaCancelada() {
        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(OrderTermsProposal.PENDIENTE).build();
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));
        given(propuestaTerminosPedidoRepository.save(any(OrderTermsProposal.class)))
                .willAnswer(inv -> inv.getArgument(0));

        TermsProposalResponse respuesta = pedidoServicio.cancelarPropuestaTerminos(10L, 7L, 1L);

        assertThat(respuesta.getEstado()).isEqualTo(OrderTermsProposal.CANCELADA);
    }

    @Test
    @DisplayName("cancelarPropuestaTerminos rechaza a quien no propuso los terminos")
    void cancelarPropuestaTerminos_rechazaNoPropietario() {
        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(OrderTermsProposal.PENDIENTE).build();
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));

        assertThatThrownBy(() -> pedidoServicio.cancelarPropuestaTerminos(10L, 7L, 999L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Solo quien propuso");
        verify(propuestaTerminosPedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("obtenerPropuestaPendienteDelPedido — propuesta inexistente lanza excepcion")
    void propuestaTerminos_propuestaInexistente_lanzaExcepcion() {
        given(propuestaTerminosPedidoRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.cancelarPropuestaTerminos(10L, 99L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Propuesta no encontrada");
    }

    @Test
    @DisplayName("obtenerPropuestaPendienteDelPedido — propuesta de otro pedido lanza excepcion")
    void propuestaTerminos_propuestaDeOtroPedido_lanzaExcepcion() {
        Order otroPedido = Order.builder().idPedido(20L).build();
        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .idPropuesta(7L).pedido(otroPedido).propuestoPor(cliente)
                .estado(OrderTermsProposal.PENDIENTE).build();
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));

        assertThatThrownBy(() -> pedidoServicio.cancelarPropuestaTerminos(10L, 7L, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Propuesta no encontrada");
    }

    @Test
    @DisplayName("obtenerPropuestaPendienteDelPedido — propuesta ya resuelta lanza excepcion")
    void propuestaTerminos_propuestaYaResuelta_lanzaExcepcion() {
        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .estado(OrderTermsProposal.ACEPTADA).build();
        given(propuestaTerminosPedidoRepository.findById(7L)).willReturn(Optional.of(propuesta));

        assertThatThrownBy(() -> pedidoServicio.cancelarPropuestaTerminos(10L, 7L, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya fue resuelta");
    }

    @Test
    @DisplayName("obtenerPropuestaPendiente — devuelve la propuesta pendiente del pedido")
    void obtenerPropuestaPendiente_devuelvePropuesta() {
        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .idPropuesta(7L).pedido(pedido).propuestoPor(cliente)
                .precioPropuesto(new BigDecimal("35.00")).estado(OrderTermsProposal.PENDIENTE).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findByPedidoIdPedidoAndEstado(10L, OrderTermsProposal.PENDIENTE))
                .willReturn(Optional.of(propuesta));

        TermsProposalResponse respuesta = pedidoServicio.obtenerPropuestaPendiente(10L, 1L);

        assertThat(respuesta.getIdPropuesta()).isEqualTo(7L);
    }

    @Test
    @DisplayName("obtenerPropuestaPendiente — sin propuesta pendiente lanza excepcion")
    void obtenerPropuestaPendiente_sinPropuesta_lanzaExcepcion() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(propuestaTerminosPedidoRepository.findByPedidoIdPedidoAndEstado(10L, OrderTermsProposal.PENDIENTE))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.obtenerPropuestaPendiente(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("obtenerPropuestaPendiente — usuario ajeno al pedido recibe AccessDenied")
    void obtenerPropuestaPendiente_usuarioAjeno_lanzaAccessDenied() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoServicio.obtenerPropuestaPendiente(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---------- obtenerPedidoPorId (IDOR, OBS-08) ----------

    @Test
    @DisplayName("obtenerPedidoPorId permite al cliente dueño consultar su pedido")
    void obtenerPedidoPorId_clientePuedeVer() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        assertThat(pedidoServicio.obtenerPedidoPorId(10L, 1L)).isNotNull();
    }

    @Test
    @DisplayName("obtenerPedidoPorId permite al creador del servicio consultar el pedido")
    void obtenerPedidoPorId_creadorPuedeVer() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        assertThat(pedidoServicio.obtenerPedidoPorId(10L, 2L)).isNotNull();
    }

    @Test
    @DisplayName("obtenerPedidoPorId permite a un ADMIN autenticado consultar pedidos ajenos")
    void obtenerPedidoPorId_adminPuedeVer() {
        autenticarComo("admin@test.com", "ROLE_ADMIN");
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        assertThat(pedidoServicio.obtenerPedidoPorId(10L, 999L)).isNotNull();
    }

    @Test
    @DisplayName("1.2: obtenerPedidoPorId deriva etapaActual del último elemento del historial, sin consulta aparte")
    void obtenerPedidoPorId_etapaActualDelHistorial() {
        OrderStatusHistory h1 = OrderStatusHistory.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        WorkflowStage etapaRevision = WorkflowStage.builder().idEtapa(2L).nombreEtapa("Revisión").build();
        OrderStatusHistory h2 = OrderStatusHistory.builder().idHistorialEstado(2L).pedido(pedido).etapa(etapaRevision).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of(h1, h2));

        OrderResponse respuesta = pedidoServicio.obtenerPedidoPorId(10L, 1L);

        assertThat(respuesta.getEtapaActual()).isEqualTo("Revisión");
        verify(historialRepository, never()).findTopByPedidoIdPedidoOrderByFechaTransicionDesc(any());
    }

    @Test
    @DisplayName("1.2: obtenerPedidoPorId devuelve \"Sin estado\" cuando el pedido no tiene historial")
    void obtenerPedidoPorId_sinHistorial_etapaActualSinEstado() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        OrderResponse respuesta = pedidoServicio.obtenerPedidoPorId(10L, 1L);

        assertThat(respuesta.getEtapaActual()).isEqualTo("Sin estado");
    }

    @Test
    @DisplayName("obtenerPedidoPorId rechaza a un usuario ajeno sin rol admin")
    void obtenerPedidoPorId_rechazaAjeno() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoServicio.obtenerPedidoPorId(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("obtenerPedidoPorId lanza recurso no encontrado si el pedido no existe")
    void obtenerPedidoPorId_inexistente() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.obtenerPedidoPorId(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- listarMisPedidos / listarMisComisiones ----------

    @Test
    @DisplayName("listarMisPedidos mapea los pedidos del cliente")
    void listarMisPedidos_mapea() {
        given(pedidoRepository.findByUsuarioClienteIdUsuario(1L)).willReturn(List.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.empty());

        List<OrderSummaryResponse> resultado = pedidoServicio.listarMisPedidos(1L);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("listarMisComisiones mapea los pedidos del creador")
    void listarMisComisiones_mapea() {
        given(pedidoRepository.findByServicioPerfilUsuarioIdUsuario(2L)).willReturn(List.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.empty());

        assertThat(pedidoServicio.listarMisComisiones(2L)).hasSize(1);
    }

    // ---------- exportarMisComisiones (1.4) ----------

    @Test
    @DisplayName("1.4: exportarMisComisiones sin idsPedido exporta todas las comisiones del creador")
    void exportarMisComisiones_sinIds_exportaTodas() {
        Order pedidoOtro = Order.builder().idPedido(11L).usuarioCliente(cliente).servicio(servicio)
                .flujo(flujo).precioPactado(new BigDecimal("30.00")).build();
        given(pedidoRepository.findByServicioPerfilUsuarioIdUsuario(2L)).willReturn(List.of(pedido, pedidoOtro));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(any())).willReturn(Optional.empty());
        given(servicioExportacion.exportar(any(), any()))
                .willReturn(new GeneratedDocument(new byte[0], "text/csv", "comisiones.csv"));

        pedidoServicio.exportarMisComisiones(2L, null, ReportFormat.CSV, "creador@test.dev");

        org.mockito.ArgumentCaptor<ReportModel> captor = org.mockito.ArgumentCaptor.forClass(ReportModel.class);
        verify(servicioExportacion).exportar(captor.capture(), org.mockito.ArgumentMatchers.eq(ReportFormat.CSV));
        assertThat(captor.getValue().getFilas()).hasSize(2);
    }

    @Test
    @DisplayName("1.4: exportarMisComisiones con idsPedido exporta exactamente lo filtrado en pantalla")
    void exportarMisComisiones_conIds_exportaSoloEsosPedidos() {
        Order pedidoOtro = Order.builder().idPedido(11L).usuarioCliente(cliente).servicio(servicio)
                .flujo(flujo).precioPactado(new BigDecimal("30.00")).build();
        given(pedidoRepository.findByServicioPerfilUsuarioIdUsuario(2L)).willReturn(List.of(pedido, pedidoOtro));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(any())).willReturn(Optional.empty());
        given(servicioExportacion.exportar(any(), any()))
                .willReturn(new GeneratedDocument(new byte[0], "text/csv", "comisiones.csv"));

        pedidoServicio.exportarMisComisiones(2L, List.of(10L), ReportFormat.CSV, "creador@test.dev");

        org.mockito.ArgumentCaptor<ReportModel> captor = org.mockito.ArgumentCaptor.forClass(ReportModel.class);
        verify(servicioExportacion).exportar(captor.capture(), org.mockito.ArgumentMatchers.eq(ReportFormat.CSV));
        List<OrderSummaryResponse> filas = captor.getValue().getFilas();
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).getIdPedido()).isEqualTo(10L);
    }

    @Test
    @DisplayName("1.4: un id ajeno en idsPedido no filtra dentro de las comisiones del creador (no es IDOR)")
    void exportarMisComisiones_idAjeno_noAparece() {
        given(pedidoRepository.findByServicioPerfilUsuarioIdUsuario(2L)).willReturn(List.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.empty());
        given(servicioExportacion.exportar(any(), any()))
                .willReturn(new GeneratedDocument(new byte[0], "text/csv", "comisiones.csv"));

        pedidoServicio.exportarMisComisiones(2L, List.of(999L), ReportFormat.CSV, "creador@test.dev");

        org.mockito.ArgumentCaptor<ReportModel> captor = org.mockito.ArgumentCaptor.forClass(ReportModel.class);
        verify(servicioExportacion).exportar(captor.capture(), org.mockito.ArgumentMatchers.eq(ReportFormat.CSV));
        assertThat(captor.getValue().getFilas()).isEmpty();
    }

    // ---------- avanzarEtapa ----------

    @Test
    @DisplayName("avanzarEtapa registra la transicion a la siguiente etapa configurada")
    void avanzarEtapa_avanza() {
        AdvanceStageRequest peticion = AdvanceStageRequest.builder().observacion("Listo para revision").build();
        OrderStatusHistory ultimo = OrderStatusHistory.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        WorkflowStageConfig configInicial = WorkflowStageConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();
        WorkflowStageConfig configSiguiente = WorkflowStageConfig.builder().idFlujoEtapa(2L).flujo(flujo).etapa(etapaSiguiente).numeroOrden(2).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.of(ultimo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(configInicial, configSiguiente));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(1L, 1))
                .willReturn(List.of(configSiguiente));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        OrderResponse respuesta = pedidoServicio.avanzarEtapa(10L, 2L, peticion);

        assertThat(respuesta).isNotNull();
        org.mockito.Mockito.verify(historialRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("avanzarEtapa rechaza si la etapa actual exige entregable y el pedido no tiene ninguno subido")
    void avanzarEtapa_rechazaSinEntregableRequerido() {
        AdvanceStageRequest peticion = AdvanceStageRequest.builder().build();
        OrderStatusHistory ultimo = OrderStatusHistory.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        WorkflowStageConfig configInicial = WorkflowStageConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial)
                .numeroOrden(1).requiereEntregable(true).build();
        WorkflowStageConfig configSiguiente = WorkflowStageConfig.builder().idFlujoEtapa(2L).flujo(flujo).etapa(etapaSiguiente).numeroOrden(2).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.of(ultimo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(configInicial, configSiguiente));
        given(entregableFinalRepository.existsByPedidoIdPedido(10L)).willReturn(false);

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 2L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("entregable");
        verify(historialRepository, never()).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("avanzarEtapa permite avanzar si la etapa exige entregable pero ya se subio uno")
    void avanzarEtapa_permiteConEntregableSubido() {
        AdvanceStageRequest peticion = AdvanceStageRequest.builder().build();
        OrderStatusHistory ultimo = OrderStatusHistory.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        WorkflowStageConfig configInicial = WorkflowStageConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial)
                .numeroOrden(1).requiereEntregable(true).build();
        WorkflowStageConfig configSiguiente = WorkflowStageConfig.builder().idFlujoEtapa(2L).flujo(flujo).etapa(etapaSiguiente).numeroOrden(2).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.of(ultimo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(configInicial, configSiguiente));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(1L, 1))
                .willReturn(List.of(configSiguiente));
        given(entregableFinalRepository.existsByPedidoIdPedido(10L)).willReturn(true);
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        OrderResponse respuesta = pedidoServicio.avanzarEtapa(10L, 2L, peticion);

        assertThat(respuesta).isNotNull();
        verify(historialRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("avanzarEtapa rechaza a un usuario que no es el creador del servicio")
    void avanzarEtapa_rechazaNoCreador() {
        AdvanceStageRequest peticion = AdvanceStageRequest.builder().build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 999L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("avanzarEtapa rechaza si el pedido no tiene estado inicial")
    void avanzarEtapa_sinEstadoInicial() {
        AdvanceStageRequest peticion = AdvanceStageRequest.builder().build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 2L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("avanzarEtapa rechaza si ya esta en la etapa final")
    void avanzarEtapa_rechazaEtapaFinal() {
        AdvanceStageRequest peticion = AdvanceStageRequest.builder().build();
        OrderStatusHistory ultimo = OrderStatusHistory.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaSiguiente).build();
        WorkflowStageConfig configFinal = WorkflowStageConfig.builder().idFlujoEtapa(2L).flujo(flujo).etapa(etapaSiguiente).numeroOrden(2).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.of(ultimo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(configFinal));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(1L, 2))
                .willReturn(List.of());

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 2L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("avanzarEtapa rechaza en vez de retroceder si la etapa actual ya no esta en la configuracion del flujo")
    void avanzarEtapa_etapaActualYaNoEnConfig_rechazaEnVezDeRetroceder() {
        // Reproduce el escenario del bug: alguien borró la WorkflowStageConfig de
        // la etapa en la que el pedido está detenido. Antes, obtenerOrdenActual
        // caía a orden=0 y avanzarEtapa tomaba la primera etapa del flujo como
        // "siguiente" — el pedido retrocedía en silencio de la etapa 2 a la 1.
        AdvanceStageRequest peticion = AdvanceStageRequest.builder().build();
        OrderStatusHistory ultimo = OrderStatusHistory.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaSiguiente).build();
        // La config del flujo ya no incluye "etapaSiguiente" (fue eliminada).
        WorkflowStageConfig soloEtapaInicial = WorkflowStageConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(10L)).willReturn(Optional.of(ultimo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(soloEtapaInicial));

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 2L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ya no forma parte del flujo");
        org.mockito.Mockito.verify(historialRepository, never()).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("avanzarEtapa lanza recurso no encontrado si el pedido no existe")
    void avanzarEtapa_pedidoInexistente() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.avanzarEtapa(10L, 2L, AdvanceStageRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- obtenerHistorial ----------

    @Test
    @DisplayName("obtenerHistorial devuelve el historial ordenado cuando el pedido existe")
    void obtenerHistorial_devuelveLista() {
        OrderStatusHistory h = OrderStatusHistory.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of(h));

        List<StatusHistoryResponse> resultado = pedidoServicio.obtenerHistorial(10L, 1L);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("obtenerHistorial lanza recurso no encontrado si el pedido no existe")
    void obtenerHistorial_pedidoInexistente() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.obtenerHistorial(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("obtenerHistorial rechaza a un usuario ajeno sin rol admin")
    void obtenerHistorial_rechazaAjeno() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoServicio.obtenerHistorial(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("obtenerHistorial permite a un ADMIN autenticado consultar pedidos ajenos")
    void obtenerHistorial_adminPuedeVer() {
        autenticarComo("admin@test.com", "ROLE_ADMIN");
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        assertThat(pedidoServicio.obtenerHistorial(10L, 999L)).isNotNull();
    }

    // ---------- obtenerSeguimiento ----------

    @Test
    @DisplayName("obtenerSeguimiento calcula el porcentaje de avance con historial")
    void obtenerSeguimiento_calculaPorcentaje() {
        OrderStatusHistory h = OrderStatusHistory.builder().idHistorialEstado(1L).pedido(pedido).etapa(etapaInicial).build();
        WorkflowStageConfig config1 = WorkflowStageConfig.builder().idFlujoEtapa(1L).flujo(flujo).etapa(etapaInicial).numeroOrden(1).build();
        WorkflowStageConfig config2 = WorkflowStageConfig.builder().idFlujoEtapa(2L).flujo(flujo).etapa(etapaSiguiente).numeroOrden(2).build();

        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of(config1, config2));
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of(h));

        OrderTrackingResponse resultado = pedidoServicio.obtenerSeguimiento(10L, 1L);

        assertThat(resultado.getEtapaActual()).isEqualTo("Inicio");
        assertThat(resultado.getTotalEtapas()).isEqualTo(2);
        assertThat(resultado.getPorcentajeProgreso()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("obtenerSeguimiento reporta sin estado cuando no hay historial")
    void obtenerSeguimiento_sinHistorial() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        OrderTrackingResponse resultado = pedidoServicio.obtenerSeguimiento(10L, 1L);

        assertThat(resultado.getEtapaActual()).isEqualTo("Sin estado");
        assertThat(resultado.getPorcentajeProgreso()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("obtenerSeguimiento lanza recurso no encontrado si el pedido no existe")
    void obtenerSeguimiento_pedidoInexistente() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.obtenerSeguimiento(10L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("obtenerSeguimiento rechaza a un usuario ajeno sin rol admin")
    void obtenerSeguimiento_rechazaAjeno() {
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> pedidoServicio.obtenerSeguimiento(10L, 999L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("obtenerSeguimiento permite a un ADMIN autenticado consultar pedidos ajenos")
    void obtenerSeguimiento_adminPuedeVer() {
        autenticarComo("admin@test.com", "ROLE_ADMIN");
        given(pedidoRepository.findById(10L)).willReturn(Optional.of(pedido));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());
        given(historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(10L)).willReturn(List.of());

        assertThat(pedidoServicio.obtenerSeguimiento(10L, 999L)).isNotNull();
    }

    private void autenticarComo(String correo, String... authorities) {
        List<SimpleGrantedAuthority> roles = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        var auth = new UsernamePasswordAuthenticationToken(correo, "N/A", roles);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
