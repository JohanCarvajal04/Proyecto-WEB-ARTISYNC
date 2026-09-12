package uteq.edu.ec.artisync.service.pedido.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateOrderRequest;
import uteq.edu.ec.artisync.entity.catalogo.Workflow;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.pedido.WorkflowStage;
import uteq.edu.ec.artisync.entity.pedido.WorkflowStageConfig;
import uteq.edu.ec.artisync.entity.pedido.OrderStatusHistory;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.WorkflowRepository;
import uteq.edu.ec.artisync.repository.catalogo.OfferingRepository;
import uteq.edu.ec.artisync.repository.pedido.WorkflowStageConfigRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderStatusHistoryRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * El flujo del pedido sale del servicio, elegido por su creador entre sus
 * propios flujos (antes, RF-19, salía de la categoría del servicio).
 *
 * <p>Si el servicio no tiene flujo asignado, cae primero al flujo más antiguo
 * del propio creador, y si el creador tampoco tiene ninguno, a un flujo por
 * defecto global -- así un catálogo a medio configurar nunca bloquea la
 * creación de un pedido.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceImplWorkflowTest {

    @Mock private OrderRepository pedidoRepository;
    @Mock private OfferingRepository servicioRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private WorkflowRepository flujoTrabajoRepository;
    @Mock private WorkflowStageConfigRepository flujoEtapaConfigRepository;
    @Mock private OrderStatusHistoryRepository historialRepository;
    @Mock private uteq.edu.ec.artisync.repository.pedido.WorkflowStageRepository etapaFlujoRepository;
    @Mock private uteq.edu.ec.artisync.service.comunicacion.ChatService chatService;
    @Mock private uteq.edu.ec.artisync.service.perfil.IVerificationService verificacionServicio;

    @InjectMocks
    private OrderServiceImpl pedidoServicio;

    private static final Long ID_CREADOR = 2L;

    private User cliente;
    private Offering servicio;
    private Workflow flujoDelServicio;
    private Workflow flujoDelCreador;
    private Workflow flujoPorDefecto;
    private CreateOrderRequest peticion;

    @BeforeEach
    void setUp() {
        cliente = User.builder().idUsuario(1L).build();

        User creador = User.builder().idUsuario(ID_CREADOR).build();
        CreatorProfile perfil = CreatorProfile.builder().idPerfil(10L).usuario(creador).build();

        servicio = Offering.builder()
                .idServicio(100L)
                .perfil(perfil)
                .precioBase(new BigDecimal("50.00"))
                .build();

        flujoDelServicio = Workflow.builder().idFlujo(20L).nombreFlujo("Flujo ilustracion").build();
        flujoDelCreador = Workflow.builder().idFlujo(15L).nombreFlujo("Flujo del creador").build();
        flujoPorDefecto = Workflow.builder().idFlujo(1L).nombreFlujo("Flujo estandar").build();

        peticion = CreateOrderRequest.builder().idServicio(100L).build();

        lenient().when(usuarioRepository.findById(1L)).thenReturn(Optional.of(cliente));
        lenient().when(servicioRepository.findById(100L)).thenReturn(Optional.of(servicio));
        lenient().when(pedidoRepository.save(any(Order.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(historialRepository.save(any(OrderStatusHistory.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(verificacionServicio.isIdentityVerified(anyLong())).thenReturn(true);
    }

    /** Configura etapas para el flujo indicado, que es lo que exige createOrder. */
    private void conEtapas(Workflow flujo) {
        WorkflowStage etapa = WorkflowStage.builder().idEtapa(1L).nombreEtapa("Briefing").build();
        WorkflowStageConfig config = WorkflowStageConfig.builder()
                .idFlujoEtapa(1L).flujo(flujo).etapa(etapa).numeroOrden(1).esEtapaFinal(false)
                .build();
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(flujo.getIdFlujo()))
                .willReturn(List.of(config));
    }

    @Test
    @DisplayName("usa el flujo configurado en el servicio")
    void usaElFlujoDelServicio() {
        servicio.setFlujo(flujoDelServicio);
        conEtapas(flujoDelServicio);

        pedidoServicio.createOrder(1L, peticion);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getFlujo().getIdFlujo()).isEqualTo(20L);
    }

    @Test
    @DisplayName("cae al flujo mas antiguo del propio creador cuando el servicio no tiene flujo asignado")
    void caeAlFlujoDelCreador() {
        servicio.setFlujo(null);
        conEtapas(flujoDelCreador);
        given(flujoTrabajoRepository.findFirstByCreadorIdUsuarioOrderByIdFlujoAsc(ID_CREADOR))
                .willReturn(Optional.of(flujoDelCreador));

        pedidoServicio.createOrder(1L, peticion);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getFlujo().getIdFlujo()).isEqualTo(15L);
    }

    @Test
    @DisplayName("cae al flujo por defecto global si ni el servicio ni su creador tienen flujo")
    void caeAlFlujoPorDefectoGlobal() {
        servicio.setFlujo(null);
        conEtapas(flujoPorDefecto);
        given(flujoTrabajoRepository.findFirstByCreadorIdUsuarioOrderByIdFlujoAsc(ID_CREADOR))
                .willReturn(Optional.empty());
        given(flujoTrabajoRepository.findFirstByOrderByIdFlujoAsc())
                .willReturn(Optional.of(flujoPorDefecto));

        pedidoServicio.createOrder(1L, peticion);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getFlujo().getIdFlujo()).isEqualTo(1L);
    }

    @Test
    @DisplayName("el respaldo no depende del nombre del flujo, que lo fija el seed")
    void respaldoIndependienteDelNombre() {
        servicio.setFlujo(null);
        Workflow conOtroNombre = Workflow.builder()
                .idFlujo(3L).nombreFlujo("Flujo Estándar de Medición").build();
        conEtapas(conOtroNombre);
        given(flujoTrabajoRepository.findFirstByCreadorIdUsuarioOrderByIdFlujoAsc(ID_CREADOR))
                .willReturn(Optional.empty());
        given(flujoTrabajoRepository.findFirstByOrderByIdFlujoAsc()).willReturn(Optional.of(conOtroNombre));

        pedidoServicio.createOrder(1L, peticion);

        ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
        verify(pedidoRepository).save(captor.capture());
        assertThat(captor.getValue().getFlujo().getIdFlujo()).isEqualTo(3L);
    }

    @Test
    @DisplayName("sin ningun flujo configurado, se rechaza el pedido")
    void sinFlujosRechaza() {
        servicio.setFlujo(null);
        given(flujoTrabajoRepository.findFirstByCreadorIdUsuarioOrderByIdFlujoAsc(ID_CREADOR))
                .willReturn(Optional.empty());
        given(flujoTrabajoRepository.findFirstByOrderByIdFlujoAsc()).willReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoServicio.createOrder(1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No hay flujos de trabajo configurados");
    }

    @Test
    @DisplayName("un flujo sin etapas configuradas se rechaza nombrando el flujo")
    void flujoSinEtapasRechaza() {
        servicio.setFlujo(flujoDelServicio);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(anyLong()))
                .willReturn(List.of());

        assertThatThrownBy(() -> pedidoServicio.createOrder(1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Flujo ilustracion");
    }
}
