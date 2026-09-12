package uteq.edu.ec.artisync.service.pedido.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateWorkflowRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.StageConfigRequest;
import uteq.edu.ec.artisync.dto.respuesta.pedido.WorkflowResponse;
import uteq.edu.ec.artisync.entity.catalogo.Workflow;
import uteq.edu.ec.artisync.entity.pedido.WorkflowStage;
import uteq.edu.ec.artisync.entity.pedido.WorkflowStageConfig;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.WorkflowRepository;
import uteq.edu.ec.artisync.repository.pedido.WorkflowStageRepository;
import uteq.edu.ec.artisync.repository.pedido.WorkflowStageConfigRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderStatusHistoryRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.entity.seguridad.User;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceImplTest {

    @Mock private WorkflowRepository flujoTrabajoRepository;
    @Mock private WorkflowStageRepository etapaFlujoRepository;
    @Mock private WorkflowStageConfigRepository flujoEtapaConfigRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private OrderStatusHistoryRepository historialEstadoPedidoRepository;

    @InjectMocks
    private WorkflowServiceImpl flujoTrabajoServicio;

    private Workflow flujo;
    private User creador;

    @BeforeEach
    void setUp() {
        creador = User.builder().idUsuario(10L).nombres("Test").build();
        flujo = Workflow.builder().idFlujo(1L).nombreFlujo("Flujo estandar").descripcionFlujo("desc").creador(creador).build();
    }

    @Test
    @DisplayName("createWorkflow guarda el flujo sin etapas cuando no se proporcionan")
    void crearFlujoTrabajo_sinEtapas() {
        CreateWorkflowRequest peticion = CreateWorkflowRequest.builder().nombreFlujo("Flujo estandar").build();
        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo estandar", 10L)).willReturn(false);
        given(usuarioRepository.findById(10L)).willReturn(Optional.of(creador));
        given(flujoTrabajoRepository.save(any(Workflow.class))).willReturn(flujo);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        WorkflowResponse respuesta = flujoTrabajoServicio.createWorkflow(10L, peticion);

        assertThat(respuesta.getNombreFlujo()).isEqualTo("Flujo estandar");
        verify(flujoEtapaConfigRepository, never()).save(any());
    }

    @Test
    @DisplayName("createWorkflow rechaza un nombre duplicado")
    void crearFlujoTrabajo_rechazaDuplicado() {
        CreateWorkflowRequest peticion = CreateWorkflowRequest.builder().nombreFlujo("Flujo estandar").build();
        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo estandar", 10L)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.createWorkflow(10L, peticion))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("createWorkflow rechaza dos etapas con el mismo nombre (sin distinguir mayusculas)")
    void crearFlujoTrabajo_rechazaEtapasConNombreRepetido() {
        StageConfigRequest etapa1 = StageConfigRequest.builder().nombreEtapa("Revision").numeroOrden(1).build();
        StageConfigRequest etapa2 = StageConfigRequest.builder().nombreEtapa(" REVISION").numeroOrden(2).build();
        CreateWorkflowRequest peticion = CreateWorkflowRequest.builder()
                .nombreFlujo("Flujo con etapas").etapas(List.of(etapa1, etapa2)).build();

        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo con etapas", 10L)).willReturn(false);

        assertThatThrownBy(() -> flujoTrabajoServicio.createWorkflow(10L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("repetidas");
        verify(flujoTrabajoRepository, never()).save(any());
    }

    @Test
    @DisplayName("createWorkflow rechaza dos etapas con el mismo numeroOrden")
    void crearFlujoTrabajo_rechazaEtapasConOrdenRepetido() {
        StageConfigRequest etapa1 = StageConfigRequest.builder().nombreEtapa("Revision").numeroOrden(1).build();
        StageConfigRequest etapa2 = StageConfigRequest.builder().nombreEtapa("Entrega").numeroOrden(1).build();
        CreateWorkflowRequest peticion = CreateWorkflowRequest.builder()
                .nombreFlujo("Flujo con etapas").etapas(List.of(etapa1, etapa2)).build();

        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo con etapas", 10L)).willReturn(false);

        assertThatThrownBy(() -> flujoTrabajoServicio.createWorkflow(10L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mismo número de orden");
        verify(flujoTrabajoRepository, never()).save(any());
    }

    @Test
    @DisplayName("createWorkflow crea las etapas indicadas reutilizando etapas existentes")
    void crearFlujoTrabajo_conEtapas() {
        WorkflowStage etapa = WorkflowStage.builder().idEtapa(1L).nombreEtapa("Revision").build();
        StageConfigRequest etapaConfig = StageConfigRequest.builder().nombreEtapa("Revision").numeroOrden(1).esEtapaFinal(false).build();
        CreateWorkflowRequest peticion = CreateWorkflowRequest.builder()
                .nombreFlujo("Flujo con etapas").etapas(List.of(etapaConfig)).build();

        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo con etapas", 10L)).willReturn(false);
        given(usuarioRepository.findById(10L)).willReturn(Optional.of(creador));
        given(flujoTrabajoRepository.save(any(Workflow.class))).willReturn(flujo);
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.createWorkflow(10L, peticion);

        verify(flujoEtapaConfigRepository).save(any(WorkflowStageConfig.class));
        verify(etapaFlujoRepository, never()).save(any());
    }

    @Test
    @DisplayName("createWorkflow crea una etapa nueva si no existe todavia")
    void crearFlujoTrabajo_creaEtapaNueva() {
        WorkflowStage nueva = WorkflowStage.builder().idEtapa(2L).nombreEtapa("Entrega").build();
        StageConfigRequest etapaConfig = StageConfigRequest.builder().nombreEtapa("Entrega").numeroOrden(1).esEtapaFinal(true).build();
        CreateWorkflowRequest peticion = CreateWorkflowRequest.builder()
                .nombreFlujo("Flujo con etapas").etapas(List.of(etapaConfig)).build();

        given(flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario("Flujo con etapas", 10L)).willReturn(false);
        given(usuarioRepository.findById(10L)).willReturn(Optional.of(creador));
        given(flujoTrabajoRepository.save(any(Workflow.class))).willReturn(flujo);
        given(etapaFlujoRepository.findByNombreEtapa("Entrega")).willReturn(Optional.empty());
        given(etapaFlujoRepository.save(any(WorkflowStage.class))).willReturn(nueva);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.createWorkflow(10L, peticion);

        verify(etapaFlujoRepository).save(any(WorkflowStage.class));
    }

    @Test
    @DisplayName("listWorkflows mapea solo los del creador cuando no puede ver todos")
    void listarFlujosTrabajo_mapea() {
        given(flujoTrabajoRepository.findByCreadorIdUsuario(10L)).willReturn(List.of(flujo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        assertThat(flujoTrabajoServicio.listWorkflows(10L, false)).hasSize(1);
    }

    @Test
    @DisplayName("listWorkflows devuelve los de todos los creadores cuando puedeVerTodos=true (FLUJO_MODERAR)")
    void listarFlujosTrabajo_puedeVerTodos_listaTodos() {
        Workflow flujoDeOtro = Workflow.builder().idFlujo(2L).nombreFlujo("Otro").creador(
                User.builder().idUsuario(99L).nombres("Otra").apellidos("Persona").build()).build();
        given(flujoTrabajoRepository.findAllByOrderByIdFlujoAsc()).willReturn(List.of(flujo, flujoDeOtro));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(2L)).willReturn(List.of());

        List<WorkflowResponse> resultado = flujoTrabajoServicio.listWorkflows(10L, true);

        assertThat(resultado).hasSize(2);
        verify(flujoTrabajoRepository, never()).findByCreadorIdUsuario(any());
    }

    @Test
    @DisplayName("getWorkflowById lanza recurso no encontrado si no existe")
    void obtenerFlujoPorId_inexistente() {
        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> flujoTrabajoServicio.getWorkflowById(1L, 10L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getWorkflowById accede al flujo de otro creador cuando puedeVerTodos=true")
    void obtenerFlujoPorId_puedeVerTodos_accedeAFlujoAjeno() {
        given(flujoTrabajoRepository.findById(1L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        WorkflowResponse respuesta = flujoTrabajoServicio.getWorkflowById(1L, 999L, true);

        assertThat(respuesta.getIdFlujo()).isEqualTo(1L);
        verify(flujoTrabajoRepository, never()).findByIdFlujoAndCreadorIdUsuario(any(), any());
    }

    @Test
    @DisplayName("updateWorkflow cambia nombre y descripcion")
    void actualizarFlujoTrabajo_cambiaDatos() {
        CreateWorkflowRequest peticion = CreateWorkflowRequest.builder()
                .nombreFlujo("Renombrado").descripcionFlujo("nueva desc").build();
        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        WorkflowResponse respuesta = flujoTrabajoServicio.updateWorkflow(1L, 10L, false, peticion);

        assertThat(respuesta.getNombreFlujo()).isEqualTo("Renombrado");
    }

    @Test
    @DisplayName("addStage rechaza una etapa duplicada en el flujo")
    void agregarEtapa_rechazaDuplicada() {
        WorkflowStage etapa = WorkflowStage.builder().idEtapa(1L).nombreEtapa("Revision").build();
        StageConfigRequest peticion = StageConfigRequest.builder().nombreEtapa("Revision").numeroOrden(1).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(1L, 1L)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.addStage(1L, 10L, false, peticion))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("addStage guarda la nueva configuracion cuando no esta duplicada")
    void agregarEtapa_guarda() {
        WorkflowStage etapa = WorkflowStage.builder().idEtapa(1L).nombreEtapa("Revision").build();
        StageConfigRequest peticion = StageConfigRequest.builder().nombreEtapa("Revision").numeroOrden(1).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(1L, 1L)).willReturn(false);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        assertThat(flujoTrabajoServicio.addStage(1L, 10L, false, peticion)).isNotNull();
        verify(flujoEtapaConfigRepository).save(any(WorkflowStageConfig.class));
    }

    @Test
    @DisplayName("addStage rechaza un numeroOrden ya usado por otra etapa del flujo")
    void agregarEtapa_rechazaOrdenDuplicado() {
        WorkflowStage etapa = WorkflowStage.builder().idEtapa(1L).nombreEtapa("Revision").build();
        StageConfigRequest peticion = StageConfigRequest.builder().nombreEtapa("Revision").numeroOrden(1).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(etapaFlujoRepository.findByNombreEtapa("Revision")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(1L, 1L)).willReturn(false);
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndNumeroOrden(1L, 1)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.addStage(1L, 10L, false, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("número de orden");
        verify(flujoEtapaConfigRepository, never()).save(any(WorkflowStageConfig.class));
    }

    @Test
    @DisplayName("addStage propaga requiereEntregable a la configuracion guardada")
    void agregarEtapa_propagaRequiereEntregable() {
        WorkflowStage etapa = WorkflowStage.builder().idEtapa(1L).nombreEtapa("Entrega de trabajo").build();
        StageConfigRequest peticion = StageConfigRequest.builder()
                .nombreEtapa("Entrega de trabajo").numeroOrden(1).requiereEntregable(true).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(etapaFlujoRepository.findByNombreEtapa("Entrega de trabajo")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(1L, 1L)).willReturn(false);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.addStage(1L, 10L, false, peticion);

        ArgumentCaptor<WorkflowStageConfig> captor = ArgumentCaptor.forClass(WorkflowStageConfig.class);
        verify(flujoEtapaConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getRequiereEntregable()).isTrue();
    }

    @Test
    @DisplayName("agregarEtapa propaga requiereBoceto a la configuracion guardada")
    void agregarEtapa_propagaRequiereBoceto() {
        WorkflowStage etapa = WorkflowStage.builder().idEtapa(1L).nombreEtapa("Boceto inicial").build();
        StageConfigRequest peticion = StageConfigRequest.builder()
                .nombreEtapa("Boceto inicial").numeroOrden(1).requiereBoceto(true).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(etapaFlujoRepository.findByNombreEtapa("Boceto inicial")).willReturn(Optional.of(etapa));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(1L, 1L)).willReturn(false);
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.addStage(1L, 10L, false, peticion);

        ArgumentCaptor<WorkflowStageConfig> captor = ArgumentCaptor.forClass(WorkflowStageConfig.class);
        verify(flujoEtapaConfigRepository).save(captor.capture());
        assertThat(captor.getValue().getRequiereBoceto()).isTrue();
    }

    @Test
    @DisplayName("updateStage cambia orden y marca final cuando pertenece al flujo")
    void actualizarEtapa_cambiaDatos() {
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(5L).flujo(flujo).numeroOrden(1).esEtapaFinal(false).build();
        StageConfigRequest peticion = StageConfigRequest.builder().numeroOrden(2).esEtapaFinal(true).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.updateStage(1L, 5L, 10L, false, peticion);

        assertThat(config.getNumeroOrden()).isEqualTo(2);
        assertThat(config.getEsEtapaFinal()).isTrue();
    }

    @Test
    @DisplayName("updateStage rechaza una etapa que no pertenece al flujo")
    void actualizarEtapa_rechazaOtroFlujo() {
        Workflow otroFlujo = Workflow.builder().idFlujo(2L).build();
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(5L).flujo(otroFlujo).build();
        StageConfigRequest peticion = StageConfigRequest.builder().numeroOrden(2).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));

        assertThatThrownBy(() -> flujoTrabajoServicio.updateStage(1L, 5L, 10L, false, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("updateStage rechaza un numeroOrden que ya tiene otra etapa del flujo")
    void actualizarEtapa_rechazaOrdenDuplicado() {
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(5L).flujo(flujo).numeroOrden(1).esEtapaFinal(false).build();
        StageConfigRequest peticion = StageConfigRequest.builder().numeroOrden(2).esEtapaFinal(false).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndNumeroOrden(1L, 2)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.updateStage(1L, 5L, 10L, false, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("número de orden");
        verify(flujoEtapaConfigRepository, never()).save(any(WorkflowStageConfig.class));
    }

    @Test
    @DisplayName("updateStage permite reenviar el mismo numeroOrden (alternarEtapaFinal no cambia el orden)")
    void actualizarEtapa_mismoOrden_noValidaColision() {
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(5L).flujo(flujo).numeroOrden(1).esEtapaFinal(false).build();
        StageConfigRequest peticion = StageConfigRequest.builder().numeroOrden(1).esEtapaFinal(true).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.updateStage(1L, 5L, 10L, false, peticion);

        assertThat(config.getEsEtapaFinal()).isTrue();
        verify(flujoEtapaConfigRepository, never()).existsByFlujoIdFlujoAndNumeroOrden(any(), any());
    }

    @Test
    @DisplayName("swapStageOrder intercambia el numeroOrden de las dos etapas")
    void intercambiarOrdenEtapas_intercambia() {
        WorkflowStage etapaA = WorkflowStage.builder().idEtapa(1L).nombreEtapa("Revision").build();
        WorkflowStage etapaB = WorkflowStage.builder().idEtapa(2L).nombreEtapa("Entrega").build();
        WorkflowStageConfig configA = WorkflowStageConfig.builder().idFlujoEtapa(5L).flujo(flujo).etapa(etapaA).numeroOrden(1).build();
        WorkflowStageConfig configB = WorkflowStageConfig.builder().idFlujoEtapa(6L).flujo(flujo).etapa(etapaB).numeroOrden(2).build();
        uteq.edu.ec.artisync.dto.peticion.pedido.SwapStagesRequest peticion =
                uteq.edu.ec.artisync.dto.peticion.pedido.SwapStagesRequest.builder()
                        .idFlujoEtapaA(5L).idFlujoEtapaB(6L).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(configA));
        given(flujoEtapaConfigRepository.findById(6L)).willReturn(Optional.of(configB));
        given(flujoEtapaConfigRepository.findByFlujoIdFlujoOrderByNumeroOrdenAsc(1L)).willReturn(List.of());

        flujoTrabajoServicio.swapStageOrder(1L, 10L, false, peticion);

        assertThat(configA.getNumeroOrden()).isEqualTo(2);
        assertThat(configB.getNumeroOrden()).isEqualTo(1);
        verify(flujoEtapaConfigRepository).save(configA);
        verify(flujoEtapaConfigRepository).save(configB);
    }

    @Test
    @DisplayName("swapStageOrder rechaza intercambiar una etapa consigo misma")
    void intercambiarOrdenEtapas_rechazaMismaEtapa() {
        uteq.edu.ec.artisync.dto.peticion.pedido.SwapStagesRequest peticion =
                uteq.edu.ec.artisync.dto.peticion.pedido.SwapStagesRequest.builder()
                        .idFlujoEtapaA(5L).idFlujoEtapaB(5L).build();
        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));

        assertThatThrownBy(() -> flujoTrabajoServicio.swapStageOrder(1L, 10L, false, peticion))
                .isInstanceOf(BusinessRuleException.class);
        verify(flujoEtapaConfigRepository, never()).findById(any());
    }

    @Test
    @DisplayName("swapStageOrder rechaza si una etapa no pertenece al flujo")
    void intercambiarOrdenEtapas_rechazaEtapaDeOtroFlujo() {
        Workflow otroFlujo = Workflow.builder().idFlujo(2L).build();
        WorkflowStageConfig configA = WorkflowStageConfig.builder().idFlujoEtapa(5L).flujo(flujo).numeroOrden(1).build();
        WorkflowStageConfig configB = WorkflowStageConfig.builder().idFlujoEtapa(6L).flujo(otroFlujo).numeroOrden(2).build();
        uteq.edu.ec.artisync.dto.peticion.pedido.SwapStagesRequest peticion =
                uteq.edu.ec.artisync.dto.peticion.pedido.SwapStagesRequest.builder()
                        .idFlujoEtapaA(5L).idFlujoEtapaB(6L).build();

        given(flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(1L, 10L)).willReturn(Optional.of(flujo));
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(configA));
        given(flujoEtapaConfigRepository.findById(6L)).willReturn(Optional.of(configB));

        assertThatThrownBy(() -> flujoTrabajoServicio.swapStageOrder(1L, 10L, false, peticion))
                .isInstanceOf(BusinessRuleException.class);
        verify(flujoEtapaConfigRepository, never()).save(any(WorkflowStageConfig.class));
    }

    @Test
    @DisplayName("deleteStage borra la configuracion cuando pertenece al flujo y ningun pedido esta en ella")
    void eliminarEtapa_borra() {
        WorkflowStage etapa = WorkflowStage.builder().idEtapa(7L).nombreEtapa("Revision").build();
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(5L).flujo(flujo).etapa(etapa).build();
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(historialEstadoPedidoRepository.existePedidoEnEtapaActual(1L, 7L)).willReturn(false);

        flujoTrabajoServicio.deleteStage(1L, 5L, 10L, false);

        verify(flujoEtapaConfigRepository).delete(config);
    }

    @Test
    @DisplayName("deleteStage rechaza si hay un pedido detenido actualmente en esa etapa")
    void eliminarEtapa_rechazaConPedidoEnEtapa() {
        WorkflowStage etapa = WorkflowStage.builder().idEtapa(7L).nombreEtapa("Revision").build();
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(5L).flujo(flujo).etapa(etapa).build();
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(historialEstadoPedidoRepository.existePedidoEnEtapaActual(1L, 7L)).willReturn(true);

        assertThatThrownBy(() -> flujoTrabajoServicio.deleteStage(1L, 5L, 10L, false))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pedidos actualmente detenidos");
        verify(flujoEtapaConfigRepository, never()).delete(any(WorkflowStageConfig.class));
    }

    @Test
    @DisplayName("deleteStage rechaza una etapa que no pertenece al flujo")
    void eliminarEtapa_rechazaOtroFlujo() {
        Workflow otroFlujo = Workflow.builder().idFlujo(2L).creador(creador).build();
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(5L).flujo(otroFlujo).build();
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));

        assertThatThrownBy(() -> flujoTrabajoServicio.deleteStage(1L, 5L, 10L, false))
                .isInstanceOf(BusinessRuleException.class);
        verify(flujoEtapaConfigRepository, never()).delete(any(WorkflowStageConfig.class));
    }

    @Test
    @DisplayName("deleteStage permite borrar la etapa de un flujo ajeno cuando puedeVerTodos=true")
    void eliminarEtapa_puedeVerTodos_borraDeFlujoAjeno() {
        User otroCreador = User.builder().idUsuario(77L).nombres("Otro").build();
        Workflow flujoAjeno = Workflow.builder().idFlujo(1L).creador(otroCreador).build();
        WorkflowStage etapa = WorkflowStage.builder().idEtapa(7L).nombreEtapa("Revision").build();
        WorkflowStageConfig config = WorkflowStageConfig.builder().idFlujoEtapa(5L).flujo(flujoAjeno).etapa(etapa).build();
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.of(config));
        given(historialEstadoPedidoRepository.existePedidoEnEtapaActual(1L, 7L)).willReturn(false);

        flujoTrabajoServicio.deleteStage(1L, 5L, 10L, true);

        verify(flujoEtapaConfigRepository).delete(config);
    }

    @Test
    @DisplayName("deleteStage lanza recurso no encontrado si la configuracion no existe")
    void eliminarEtapa_inexistente() {
        given(flujoEtapaConfigRepository.findById(5L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> flujoTrabajoServicio.deleteStage(1L, 5L, 10L, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
