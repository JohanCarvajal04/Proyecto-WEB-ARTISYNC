package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateWorkflowRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.StageConfigRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.SwapStagesRequest;
import uteq.edu.ec.artisync.dto.respuesta.pedido.StageConfigResponse;
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
import uteq.edu.ec.artisync.service.pedido.IWorkflowService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowServiceImpl implements IWorkflowService {

    private final WorkflowRepository flujoTrabajoRepository;
    private final WorkflowStageRepository etapaFlujoRepository;
    private final WorkflowStageConfigRepository flujoEtapaConfigRepository;
    private final UserRepository usuarioRepository;
    private final OrderStatusHistoryRepository historialEstadoPedidoRepository;

    /**
     * Crea un flujo de trabajo para un creador, con sus etapas iniciales si vienen indicadas.
     *
     * @param idUsuario identificador del creador dueño del flujo
     * @param peticion nombre, descripción y (opcional) etapas iniciales del flujo
     * @return el flujo creado, con sus etapas ya configuradas
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el creador ya tiene un flujo con ese nombre
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si las etapas iniciales tienen un
     *         nombre o número de orden repetido
     */
    @Override
    @Transactional
    public WorkflowResponse createWorkflow(Long idUsuario, CreateWorkflowRequest peticion) {
        if (flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuario(peticion.getNombreFlujo(), idUsuario)) {
            throw new DuplicateResourceException("Ya existe un flujo de trabajo con el nombre: " + peticion.getNombreFlujo());
        }

        validateStagesNoDuplicates(peticion.getEtapas());

        uteq.edu.ec.artisync.entity.seguridad.User creador = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado"));

        Workflow flujo = Workflow.builder()
                .nombreFlujo(peticion.getNombreFlujo())
                .descripcionFlujo(peticion.getDescripcionFlujo())
                .creador(creador)
                .build();

        flujo = flujoTrabajoRepository.save(flujo);

        // Crear etapas si se proporcionaron
        if (peticion.getEtapas() != null && !peticion.getEtapas().isEmpty()) {
            for (StageConfigRequest etapaReq : peticion.getEtapas()) {
                WorkflowStage etapa = getOrCreateStage(etapaReq.getNombreEtapa());

                WorkflowStageConfig config = WorkflowStageConfig.builder()
                        .flujo(flujo)
                        .etapa(etapa)
                        .numeroOrden(etapaReq.getNumeroOrden())
                        .esEtapaFinal(etapaReq.isEsEtapaFinal())
                        .requiereEntregable(etapaReq.isRequiereEntregable())
                        .build();

                flujoEtapaConfigRepository.save(config);
            }
        }

        log.info("Flujo de trabajo '{}' creado con ID {}", flujo.getNombreFlujo(), flujo.getIdFlujo());
        return mapToRespuesta(flujo);
    }

    /**
     * @param idUsuario identificador del creador
     * @param puedeVerTodos {@code true} (FLUJO_MODERAR/ADMIN) lista todos los flujos de cualquier
     *                      creador; {@code false} lista solo los propios de {@code idUsuario}
     * @return los flujos de trabajo correspondientes
     */
    @Override
    @Transactional(readOnly = true)
    public List<WorkflowResponse> listWorkflows(Long idUsuario, boolean puedeVerTodos) {
        List<Workflow> flujos = puedeVerTodos
                ? flujoTrabajoRepository.findAllByOrderByIdFlujoAsc()
                : flujoTrabajoRepository.findByCreadorIdUsuario(idUsuario);
        return flujos.stream()
                .map(this::mapToRespuesta)
                .collect(Collectors.toList());
    }

    /**
     * @param idFlujo identificador del flujo
     * @param idUsuario identificador de quien consulta
     * @param puedeVerTodos {@code true} (FLUJO_MODERAR/ADMIN) permite acceder a cualquier flujo;
     *                      {@code false} solo a los propios de {@code idUsuario}
     * @return el flujo solicitado, con sus etapas
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el flujo no existe
     *         o no es accesible para quien consulta
     */
    @Override
    @Transactional(readOnly = true)
    public WorkflowResponse getWorkflowById(Long idFlujo, Long idUsuario, boolean puedeVerTodos) {
        return mapToRespuesta(findAccessibleWorkflow(idFlujo, idUsuario, puedeVerTodos));
    }

    /**
     * Actualiza el nombre y la descripción de un flujo de trabajo.
     *
     * @param idFlujo identificador del flujo
     * @param idUsuario identificador de quien edita
     * @param puedeVerTodos {@code true} (FLUJO_MODERAR/ADMIN) permite editar cualquier flujo;
     *                      {@code false} solo los propios de {@code idUsuario}
     * @param peticion nuevo nombre y descripción del flujo
     * @return el flujo ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el flujo no existe o no es accesible
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el dueño real del flujo ya
     *         tiene otro flujo con ese nombre
     */
    @Override
    @Transactional
    public WorkflowResponse updateWorkflow(Long idFlujo, Long idUsuario, boolean puedeVerTodos, CreateWorkflowRequest peticion) {
        Workflow flujo = findAccessibleWorkflow(idFlujo, idUsuario, puedeVerTodos);

        // La unicidad de nombre es por dueño real del flujo (V25:
        // UNIQUE(id_usuario_creador, nombre_flujo)), no por quien lo edita.
        if (flujoTrabajoRepository.existsByNombreFlujoAndCreadorIdUsuarioAndIdFlujoNot(
                peticion.getNombreFlujo(), flujo.getCreador().getIdUsuario(), idFlujo)) {
            throw new DuplicateResourceException("Ya existe un flujo de trabajo con el nombre: " + peticion.getNombreFlujo());
        }

        flujo.setNombreFlujo(peticion.getNombreFlujo());
        flujo.setDescripcionFlujo(peticion.getDescripcionFlujo());
        flujoTrabajoRepository.save(flujo);

        log.info("Flujo de trabajo '{}' actualizado", flujo.getNombreFlujo());
        return mapToRespuesta(flujo);
    }

    /**
     * Agrega una etapa a un flujo (reutilizando la etapa maestra si ya existe
     * una con ese nombre).
     *
     * @param idFlujo identificador del flujo
     * @param idUsuario identificador de quien edita
     * @param puedeVerTodos {@code true} (FLUJO_MODERAR/ADMIN) permite editar cualquier flujo;
     *                      {@code false} solo los propios de {@code idUsuario}
     * @param peticion nombre, número de orden y flags de la nueva etapa
     * @return el flujo con la etapa ya agregada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el flujo no existe o no es accesible
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si esa etapa ya existe en el flujo
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya hay otra etapa en ese número de orden
     */
    @Override
    @Transactional
    public WorkflowResponse addStage(Long idFlujo, Long idUsuario, boolean puedeVerTodos, StageConfigRequest peticion) {
        Workflow flujo = findAccessibleWorkflow(idFlujo, idUsuario, puedeVerTodos);

        WorkflowStage etapa = getOrCreateStage(peticion.getNombreEtapa());

        if (flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(idFlujo, etapa.getIdEtapa())) {
            throw new DuplicateResourceException("La etapa '" + peticion.getNombreEtapa() + "' ya existe en este flujo");
        }

        if (flujoEtapaConfigRepository.existsByFlujoIdFlujoAndNumeroOrden(idFlujo, peticion.getNumeroOrden())) {
            throw new BusinessRuleException(
                    "Ya hay una etapa con el número de orden " + peticion.getNumeroOrden() + " en este flujo.");
        }

        WorkflowStageConfig config = WorkflowStageConfig.builder()
                .flujo(flujo)
                .etapa(etapa)
                .numeroOrden(peticion.getNumeroOrden())
                .esEtapaFinal(peticion.isEsEtapaFinal())
                .requiereEntregable(peticion.isRequiereEntregable())
                .build();

        flujoEtapaConfigRepository.save(config);
        log.info("Etapa '{}' agregada al flujo '{}'", etapa.getNombreEtapa(), flujo.getNombreFlujo());

        return mapToRespuesta(flujo);
    }

    /**
     * Actualiza la configuración (orden, si es final, si exige entregable) de
     * una etapa ya agregada a un flujo.
     *
     * @param idFlujo identificador del flujo
     * @param idFlujoEtapa identificador de la configuración de etapa a actualizar
     * @param idUsuario identificador de quien edita
     * @param puedeVerTodos {@code true} (FLUJO_MODERAR/ADMIN) permite editar cualquier flujo;
     *                      {@code false} solo los propios de {@code idUsuario}
     * @param peticion nuevo número de orden y flags de la etapa
     * @return el flujo con la etapa ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el flujo, la configuración de
     *         etapa no existen, o el flujo no es accesible
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la etapa no pertenece a ese flujo,
     *         o si el nuevo orden ya lo ocupa otra etapa del mismo flujo
     */
    @Override
    @Transactional
    public WorkflowResponse updateStage(Long idFlujo, Long idFlujoEtapa, Long idUsuario, boolean puedeVerTodos, StageConfigRequest peticion) {
        Workflow flujo = findAccessibleWorkflow(idFlujo, idUsuario, puedeVerTodos);

        WorkflowStageConfig config = flujoEtapaConfigRepository.findById(idFlujoEtapa)
                .orElseThrow(() -> new ResourceNotFoundException("Configuracion de etapa no encontrada"));

        if (!config.getFlujo().getIdFlujo().equals(idFlujo)) {
            throw new BusinessRuleException("La etapa no pertenece al flujo especificado");
        }

        // Solo valida si el orden realmente cambia: alternarEtapaFinal reenvía
        // el mismo numeroOrden en cada toggle, y compararlo contra sí mismo
        // siempre "colisionaría". Reordenar de verdad usa swapStageOrder,
        // que hace el swap atómico — este chequeo es para llamadas directas a la
        // API que intenten mover una etapa a un orden ya ocupado por OTRA.
        if (!config.getNumeroOrden().equals(peticion.getNumeroOrden())
                && flujoEtapaConfigRepository.existsByFlujoIdFlujoAndNumeroOrden(idFlujo, peticion.getNumeroOrden())) {
            throw new BusinessRuleException(
                    "Ya hay una etapa con el número de orden " + peticion.getNumeroOrden() + " en este flujo.");
        }

        config.setNumeroOrden(peticion.getNumeroOrden());
        config.setEsEtapaFinal(peticion.isEsEtapaFinal());
        config.setRequiereEntregable(peticion.isRequiereEntregable());

        flujoEtapaConfigRepository.save(config);
        log.info("Etapa {} actualizada en flujo {}", idFlujoEtapa, idFlujo);

        return mapToRespuesta(flujo);
    }

    /**
     * Intercambia el número de orden entre dos etapas del mismo flujo (swap atómico).
     *
     * @param idFlujo identificador del flujo
     * @param idUsuario identificador de quien edita
     * @param puedeVerTodos {@code true} (FLUJO_MODERAR/ADMIN) permite editar cualquier flujo;
     *                      {@code false} solo los propios de {@code idUsuario}
     * @param peticion identificadores de las dos configuraciones de etapa a intercambiar
     * @return el flujo con el orden ya intercambiado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el flujo o alguna configuración
     *         de etapa no existen, o el flujo no es accesible
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ambos identificadores son el mismo,
     *         o si alguna etapa no pertenece a ese flujo
     */
    @Override
    @Transactional
    public WorkflowResponse swapStageOrder(Long idFlujo, Long idUsuario, boolean puedeVerTodos, SwapStagesRequest peticion) {
        Workflow flujo = findAccessibleWorkflow(idFlujo, idUsuario, puedeVerTodos);

        if (peticion.getIdFlujoEtapaA().equals(peticion.getIdFlujoEtapaB())) {
            throw new BusinessRuleException("No se puede intercambiar una etapa consigo misma");
        }

        WorkflowStageConfig a = flujoEtapaConfigRepository.findById(peticion.getIdFlujoEtapaA())
                .orElseThrow(() -> new ResourceNotFoundException("Configuracion de etapa no encontrada"));
        WorkflowStageConfig b = flujoEtapaConfigRepository.findById(peticion.getIdFlujoEtapaB())
                .orElseThrow(() -> new ResourceNotFoundException("Configuracion de etapa no encontrada"));

        if (!a.getFlujo().getIdFlujo().equals(idFlujo) || !b.getFlujo().getIdFlujo().equals(idFlujo)) {
            throw new BusinessRuleException("Las etapas no pertenecen al flujo especificado");
        }

        Integer ordenA = a.getNumeroOrden();
        a.setNumeroOrden(b.getNumeroOrden());
        b.setNumeroOrden(ordenA);
        flujoEtapaConfigRepository.save(a);
        flujoEtapaConfigRepository.save(b);

        log.info("Etapas {} y {} intercambiaron orden en flujo {}", a.getIdFlujoEtapa(), b.getIdFlujoEtapa(), idFlujo);
        return mapToRespuesta(flujo);
    }

    /**
     * Elimina una etapa de un flujo, siempre que ningún pedido esté
     * actualmente detenido en ella.
     *
     * @param idFlujo identificador del flujo
     * @param idFlujoEtapa identificador de la configuración de etapa a eliminar
     * @param idUsuario identificador de quien edita
     * @param puedeVerTodos {@code true} (FLUJO_MODERAR/ADMIN) permite editar cualquier flujo;
     *                      {@code false} solo los propios de {@code idUsuario}
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la configuración de etapa no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la etapa no pertenece a ese flujo,
     *         si quien llama no tiene permiso, o si hay pedidos detenidos en esa etapa
     */
    @Override
    @Transactional
    public void deleteStage(Long idFlujo, Long idFlujoEtapa, Long idUsuario, boolean puedeVerTodos) {
        WorkflowStageConfig config = flujoEtapaConfigRepository.findById(idFlujoEtapa)
                .orElseThrow(() -> new ResourceNotFoundException("Configuracion de etapa no encontrada"));

        boolean esDueno = config.getFlujo().getCreador().getIdUsuario().equals(idUsuario);
        if (!config.getFlujo().getIdFlujo().equals(idFlujo) || (!esDueno && !puedeVerTodos)) {
            throw new BusinessRuleException("La etapa no pertenece al flujo especificado o no tiene permisos");
        }

        // Un pedido detenido en esta etapa dejaría de encontrarla en la
        // configuración del flujo al borrarla, y "retrocedería" a la primera
        // etapa en el siguiente avance (OrderServiceImpl.obtenerOrdenActual).
        if (historialEstadoPedidoRepository.existePedidoEnEtapaActual(idFlujo, config.getEtapa().getIdEtapa())) {
            throw new BusinessRuleException(
                    "No se puede eliminar la etapa '" + config.getEtapa().getNombreEtapa()
                            + "': hay pedidos actualmente detenidos en ella.");
        }

        flujoEtapaConfigRepository.delete(config);
        log.info("Etapa {} eliminada del flujo {}", idFlujoEtapa, idFlujo);
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    /** Con puedeVerTodos=true (FLUJO_MODERAR/ADMIN) accede a cualquier flujo; si no, solo a los propios. */
    private Workflow findAccessibleWorkflow(Long idFlujo, Long idUsuario, boolean puedeVerTodos) {
        if (puedeVerTodos) {
            return flujoTrabajoRepository.findById(idFlujo)
                    .orElseThrow(() -> new ResourceNotFoundException("Flujo de trabajo no encontrado con ID: " + idFlujo));
        }
        return flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(idFlujo, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Flujo de trabajo no encontrado con ID: " + idFlujo));
    }

    /**
     * Antes de crear un flujo con varias etapas de una vez, nada impedía
     * mandar dos con el mismo nombre o el mismo numeroOrden. Un nombre
     * repetido reventaba con un 500 crudo al chocar contra el UNIQUE
     * (id_flujo, id_etapa) de flujo_etapas_config (V25); un numeroOrden
     * repetido no tenía ninguna restricción y dejaba advanceStage eligiendo
     * entre etapas empatadas sin desempate determinista.
     */
    private void validateStagesNoDuplicates(List<StageConfigRequest> etapas) {
        if (etapas == null || etapas.isEmpty()) {
            return;
        }

        java.util.Set<String> nombresVistos = new java.util.HashSet<>();
        java.util.Set<Integer> ordenesVistos = new java.util.HashSet<>();

        for (StageConfigRequest etapa : etapas) {
            String nombreNormalizado = etapa.getNombreEtapa().trim().toLowerCase();
            if (!nombresVistos.add(nombreNormalizado)) {
                throw new BusinessRuleException(
                        "Hay etapas repetidas: '" + etapa.getNombreEtapa() + "' aparece más de una vez.");
            }
            if (!ordenesVistos.add(etapa.getNumeroOrden())) {
                throw new BusinessRuleException(
                        "Hay etapas con el mismo número de orden (" + etapa.getNumeroOrden()
                                + "): cada etapa debe tener un orden distinto.");
            }
        }
    }

    private WorkflowStage getOrCreateStage(String nombreEtapa) {
        return etapaFlujoRepository.findByNombreEtapa(nombreEtapa)
                .orElseGet(() -> {
                    WorkflowStage nueva = WorkflowStage.builder()
                            .nombreEtapa(nombreEtapa)
                            .build();
                    return etapaFlujoRepository.save(nueva);
                });
    }

    private WorkflowResponse mapToRespuesta(Workflow flujo) {
        List<WorkflowStageConfig> etapas = flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(flujo.getIdFlujo());

        return WorkflowResponse.builder()
                .idFlujo(flujo.getIdFlujo())
                .nombreFlujo(flujo.getNombreFlujo())
                .descripcionFlujo(flujo.getDescripcionFlujo())
                .etapas(etapas.stream().map(this::mapStageConfig).collect(Collectors.toList()))
                .idUsuarioCreador(flujo.getCreador().getIdUsuario())
                .nombreCreador(flujo.getCreador().getNombres() + " " + flujo.getCreador().getApellidos())
                .build();
    }

    private StageConfigResponse mapStageConfig(WorkflowStageConfig config) {
        return StageConfigResponse.builder()
                .idFlujoEtapa(config.getIdFlujoEtapa())
                .idEtapa(config.getEtapa().getIdEtapa())
                .nombreEtapa(config.getEtapa().getNombreEtapa())
                .numeroOrden(config.getNumeroOrden())
                .esEtapaFinal(config.getEsEtapaFinal())
                .requiereEntregable(config.getRequiereEntregable())
                .build();
    }
}
