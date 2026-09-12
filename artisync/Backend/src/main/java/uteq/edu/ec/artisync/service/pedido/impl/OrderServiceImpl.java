package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.AnswerBriefingRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.AdvanceStageRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateOrderRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateTermsProposalRequest;
import uteq.edu.ec.artisync.dto.respuesta.pedido.*;
import uteq.edu.ec.artisync.entity.catalogo.Workflow;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.comunicacion.SentBriefing;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingTemplate;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingQuestion;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingAnswer;
import uteq.edu.ec.artisync.entity.pedido.*;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.WorkflowRepository;
import uteq.edu.ec.artisync.repository.catalogo.OfferingRepository;
import uteq.edu.ec.artisync.repository.comunicacion.SentBriefingRepository;
import uteq.edu.ec.artisync.repository.comunicacion.BriefingAnswerRepository;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.legal.FinalDeliverableRepository;
import uteq.edu.ec.artisync.repository.pedido.*;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;
import uteq.edu.ec.artisync.service.legal.IContractService;
import uteq.edu.ec.artisync.service.pedido.IOrderService;
import uteq.edu.ec.artisync.service.perfil.IVerificationService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportColumn;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.IExportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;
import uteq.edu.ec.artisync.util.OrderOwnershipValidator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final OrderRepository pedidoRepository;
    private final OfferingRepository servicioRepository;
    private final UserRepository usuarioRepository;
    private final WorkflowRepository flujoTrabajoRepository;
    private final WorkflowStageConfigRepository flujoEtapaConfigRepository;
    private final OrderStatusHistoryRepository historialRepository;
    private final WorkflowStageRepository etapaFlujoRepository;
    private final ContractRepository contratoRepository;
    private final FinalDeliverableRepository entregableFinalRepository;
    private final SketchRepository bocetoRepository;
    private final OrderTermsProposalRepository propuestaTerminosPedidoRepository;
    private final NotificationService notificacionService;
    private final ChatService chatService;
    private final IExportService servicioExportacion;
    private final IVerificationService verificacionServicio;
    private final IContractService contratoServicio;
    private final SentBriefingRepository briefingEnviadoRepository;
    private final BriefingAnswerRepository briefingRespuestaRepository;

    /**
     * Crea un pedido de un cliente sobre un servicio, resolviendo el flujo de
     * trabajo aplicable, validando el cuestionario del servicio (si tiene uno)
     * y abriendo la sala de chat del pedido.
     *
     * @param idCliente identificador del cliente que crea el pedido
     * @param peticion servicio solicitado, precio ofrecido, fecha de entrega y respuestas al cuestionario
     * @return el pedido creado, con su primera etapa ya registrada en el historial
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el cliente o el servicio no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el cliente no tiene la identidad verificada,
     *         si intenta pedir su propio servicio, si el flujo asignado no tiene etapas configuradas,
     *         o si el servicio tiene cuestionario y falta responder alguna pregunta
     */
    @Override
    @Transactional
    @Auditable(accion = "PEDIDO_CREAR", modulo = AuditModule.PEDIDOS,
            entidad = "pedidos", idEntidad = "#resultado.idPedido",
            detalle = "{idServicio: #peticion.idServicio}")
    public OrderResponse createOrder(Long idCliente, CreateOrderRequest peticion) {
        User cliente = usuarioRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("User cliente no encontrado"));

        if (!verificacionServicio.isIdentityVerified(idCliente)) {
            throw new BusinessRuleException(
                    "Debes verificar tu identidad antes de crear un pedido. Sube tu documento de identidad desde tu perfil.");
        }

        Offering servicio = servicioRepository.findById(peticion.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado"));

        // Verificar que el cliente no sea el mismo creador del servicio
        if (servicio.getPerfil().getUsuario().getIdUsuario().equals(idCliente)) {
            throw new BusinessRuleException("No puedes crear un pedido para tu propio servicio");
        }

        Workflow flujo = resolveServiceWorkflow(servicio);

        // Verificar que el flujo tenga etapas configuradas
        List<WorkflowStageConfig> etapas = flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(flujo.getIdFlujo());
        if (etapas.isEmpty()) {
            throw new BusinessRuleException(
                    "El flujo '" + flujo.getNombreFlujo() + "' no tiene etapas configuradas");
        }

        // REQ-F-016 ampliado: si el servicio tiene un cuestionario asignado,
        // el cliente lo responde aquí mismo, antes de crear el pedido — no
        // hay un envío manual posterior del creador. Se valida ANTES de
        // guardar nada para que un cuestionario incompleto no deje un pedido
        // a medias (el método completo sigue siendo @Transactional).
        BriefingTemplate plantillaBriefing = servicio.getBriefingPlantilla();
        if (plantillaBriefing != null) {
            validateBriefingAnswersComplete(plantillaBriefing, peticion.getRespuestasBriefing());
        }

        // Crear el pedido
        Order pedido = Order.builder()
                .usuarioCliente(cliente)
                .servicio(servicio)
                .flujo(flujo)
                .precioPactado(peticion.getPrecioOfrecido() != null
                        ? peticion.getPrecioOfrecido()
                        : servicio.getPrecioBase())
                .fechaEntregaEstimada(peticion.getFechaEntregaEstimada())
                .build();

        pedido = pedidoRepository.save(pedido);

        if (plantillaBriefing != null) {
            recordBriefingCompleted(pedido, plantillaBriefing, peticion.getRespuestasBriefing());
        }

        // Registrar estado inicial (primera etapa del flujo)
        OrderStatusHistory estadoInicial = OrderStatusHistory.builder()
                .pedido(pedido)
                .etapa(etapas.get(0).getEtapa())
                .observacion("Order creado")
                .build();
        historialRepository.save(estadoInicial);

        log.info("Order {} creado por cliente {} para servicio {} con flujo '{}'",
                pedido.getIdPedido(), idCliente, peticion.getIdServicio(), flujo.getNombreFlujo());

        // La sala se abre desde ya, antes de cualquier firma: así cliente y
        // creador pueden negociar precio/alcance por chat antes de
        // comprometerse con un contrato (ver proposeTerms). Antes solo
        // se abría cuando ambas partes ya habían firmado.
        chatService.createRoom(pedido);

        return mapToRespuesta(pedido);
    }

    /**
     * Propone un cambio de precio y/o fecha de entrega sobre un pedido cuyo
     * contrato aún no tiene ninguna firma, y notifica a la otra parte.
     *
     * @param idPedido identificador del pedido
     * @param idUsuario identificador de quien propone (cliente o creador del pedido)
     * @param peticion precio y/o fecha propuestos; al menos uno debe venir informado
     * @return la propuesta creada, pendiente de aceptación o rechazo
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si no se indica ningún término,
     *         si quien llama no es parte del pedido, si el contrato ya tiene una firma,
     *         o si ya hay otra propuesta pendiente sin resolver
     */
    @Override
    @Transactional
    @Auditable(accion = "PEDIDO_PROPONER_TERMINOS", modulo = AuditModule.PEDIDOS,
            entidad = "pedidos", idEntidad = "#idPedido")
    public TermsProposalResponse proposeTerms(Long idPedido, Long idUsuario, CreateTermsProposalRequest peticion) {
        if (peticion.getPrecioPropuesto() == null && peticion.getFechaEntregaPropuesta() == null) {
            throw new BusinessRuleException("Debes indicar al menos un término a proponer");
        }

        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));

        User proponente = getOrderParty(pedido, idUsuario,
                "No tienes permiso para proponer términos de este pedido");

        validateContractUnsigned(idPedido);

        if (propuestaTerminosPedidoRepository.findByPedidoIdPedidoAndEstado(idPedido, OrderTermsProposal.PENDIENTE).isPresent()) {
            throw new BusinessRuleException(
                    "Ya existe una propuesta de cambio de términos pendiente; resuélvela antes de crear otra");
        }

        OrderTermsProposal propuesta = OrderTermsProposal.builder()
                .pedido(pedido)
                .propuestoPor(proponente)
                .precioPropuesto(peticion.getPrecioPropuesto())
                .fechaEntregaPropuesta(peticion.getFechaEntregaPropuesta())
                .build();
        propuesta = propuestaTerminosPedidoRepository.save(propuesta);

        log.info("Order {} recibió propuesta de términos {} (usuario {}): precio={}, entrega={}",
                idPedido, propuesta.getIdPropuesta(), idUsuario, propuesta.getPrecioPropuesto(), propuesta.getFechaEntregaPropuesta());

        User otraParte = getCounterparty(pedido, idUsuario);
        notificacionService.notify(otraParte, "PEDIDO_PROPUESTA_TERMINOS_CREADA",
                "Te proponen nuevos términos para el pedido \"" + pedido.getServicio().getTituloServicio() + "\".");

        return mapProposal(propuesta);
    }

    /**
     * Acepta una propuesta de términos pendiente: aplica el precio/fecha
     * propuestos al pedido y, si es la primera vez que ambas partes se ponen
     * de acuerdo, genera el contrato en el mismo paso.
     *
     * @param idPedido identificador del pedido
     * @param idPropuesta identificador de la propuesta a aceptar
     * @param idUsuario identificador de quien acepta (debe ser la otra parte, no quien propuso)
     * @return el pedido con los términos ya actualizados
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la propuesta no existe, ya fue
     *         resuelta, no pertenece a ese pedido, si quien acepta es quien la propuso,
     *         si no es parte del pedido, o si el contrato ya tiene una firma
     */
    @Override
    @Transactional
    @Auditable(accion = "PEDIDO_ACEPTAR_PROPUESTA_TERMINOS", modulo = AuditModule.PEDIDOS,
            entidad = "pedidos", idEntidad = "#idPedido")
    public OrderResponse acceptTermsProposal(Long idPedido, Long idPropuesta, Long idUsuario) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));
        OrderTermsProposal propuesta = getPendingProposalForOrder(idPedido, idPropuesta);

        if (propuesta.getPropuestoPor().getIdUsuario().equals(idUsuario)) {
            throw new BusinessRuleException("No puedes aceptar tu propia propuesta; debe hacerlo la otra parte");
        }
        getOrderParty(pedido, idUsuario, "No tienes permiso para aceptar esta propuesta");

        validateContractUnsigned(idPedido);

        if (propuesta.getPrecioPropuesto() != null) {
            pedido.setPrecioPactado(propuesta.getPrecioPropuesto());
        }
        if (propuesta.getFechaEntregaPropuesta() != null) {
            pedido.setFechaEntregaEstimada(propuesta.getFechaEntregaPropuesta());
        }
        pedido = pedidoRepository.save(pedido);

        propuesta.setEstado(OrderTermsProposal.ACEPTADA);
        propuesta.setFechaResolucion(LocalDateTime.now());
        propuestaTerminosPedidoRepository.save(propuesta);

        log.info("Order {} aceptó propuesta de términos {} (usuario {}): precio={}, entrega={}",
                idPedido, idPropuesta, idUsuario, pedido.getPrecioPactado(), pedido.getFechaEntregaEstimada());

        // El chat ofrece un atajo para generar el contrato una vez que ambas
        // partes se pusieron de acuerdo (ver ChatPedidoComponent); aceptar la
        // primera propuesta de términos ES ese acuerdo, así que el contrato se
        // genera aquí mismo con los valores recién fijados en el pedido en vez
        // de requerir un paso manual aparte.
        boolean contratoRecienGenerado = contratoRepository.findByPedidoIdPedido(idPedido).isEmpty();
        if (contratoRecienGenerado) {
            contratoServicio.generateContract(idPedido, idUsuario);
        }

        notificacionService.notify(propuesta.getPropuestoPor(),
                contratoRecienGenerado ? "PEDIDO_PROPUESTA_TERMINOS_ACEPTADA_CONTRATO_GENERADO" : "PEDIDO_PROPUESTA_TERMINOS_ACEPTADA",
                "Aceptaron tus términos propuestos para el pedido \"" + pedido.getServicio().getTituloServicio() + "\"."
                        + (contratoRecienGenerado ? " Se generó el contrato." : ""));

        return mapToRespuesta(pedido);
    }

    /**
     * Rechaza una propuesta de términos pendiente y notifica a quien la propuso.
     *
     * @param idPedido identificador del pedido
     * @param idPropuesta identificador de la propuesta a rechazar
     * @param idUsuario identificador de quien rechaza (debe ser la otra parte, no quien propuso)
     * @return la propuesta ya marcada como rechazada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la propuesta no existe, ya fue
     *         resuelta, no pertenece a ese pedido, si quien rechaza es quien la propuso, o si no es parte del pedido
     */
    @Override
    @Transactional
    public TermsProposalResponse rejectTermsProposal(Long idPedido, Long idPropuesta, Long idUsuario) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));
        OrderTermsProposal propuesta = getPendingProposalForOrder(idPedido, idPropuesta);

        if (propuesta.getPropuestoPor().getIdUsuario().equals(idUsuario)) {
            throw new BusinessRuleException("No puedes rechazar tu propia propuesta; debe hacerlo la otra parte");
        }
        getOrderParty(pedido, idUsuario, "No tienes permiso para rechazar esta propuesta");

        propuesta.setEstado(OrderTermsProposal.RECHAZADA);
        propuesta.setFechaResolucion(LocalDateTime.now());
        propuesta = propuestaTerminosPedidoRepository.save(propuesta);

        log.info("Order {} rechazó propuesta de términos {} (usuario {})", idPedido, idPropuesta, idUsuario);

        notificacionService.notify(propuesta.getPropuestoPor(), "PEDIDO_PROPUESTA_TERMINOS_RECHAZADA",
                "Rechazaron tus términos propuestos para el pedido \"" + pedido.getServicio().getTituloServicio() + "\".");

        return mapProposal(propuesta);
    }

    /**
     * Cancela una propuesta de términos pendiente. Solo puede hacerlo quien la propuso.
     *
     * @param idPedido identificador del pedido
     * @param idPropuesta identificador de la propuesta a cancelar
     * @param idUsuario identificador de quien cancela (debe ser quien la propuso)
     * @return la propuesta ya marcada como cancelada
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la propuesta no existe, ya fue
     *         resuelta, no pertenece a ese pedido, o si quien cancela no es quien la propuso
     */
    @Override
    @Transactional
    public TermsProposalResponse cancelTermsProposal(Long idPedido, Long idPropuesta, Long idUsuario) {
        OrderTermsProposal propuesta = getPendingProposalForOrder(idPedido, idPropuesta);

        if (!propuesta.getPropuestoPor().getIdUsuario().equals(idUsuario)) {
            throw new BusinessRuleException("Solo quien propuso los términos puede cancelar la propuesta");
        }

        propuesta.setEstado(OrderTermsProposal.CANCELADA);
        propuesta.setFechaResolucion(LocalDateTime.now());
        propuesta = propuestaTerminosPedidoRepository.save(propuesta);

        log.info("Order {} canceló propuesta de términos {} (usuario {})", idPedido, idPropuesta, idUsuario);

        return mapProposal(propuesta);
    }

    /**
     * Obtiene la propuesta de términos pendiente de un pedido, si la hay.
     *
     * @param idPedido identificador del pedido
     * @param idUsuarioSolicitante identificador de quien consulta, para validar que sea parte del pedido
     * @return la propuesta pendiente
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe,
     *         o si no hay ninguna propuesta pendiente
     */
    @Override
    @Transactional(readOnly = true)
    public TermsProposalResponse getPendingProposal(Long idPedido, Long idUsuarioSolicitante) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));
        OrderOwnershipValidator.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

        OrderTermsProposal propuesta = propuestaTerminosPedidoRepository
                .findByPedidoIdPedidoAndEstado(idPedido, OrderTermsProposal.PENDIENTE)
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna propuesta de términos pendiente para el pedido con ID: " + idPedido));

        return mapProposal(propuesta);
    }

    private OrderTermsProposal getPendingProposalForOrder(Long idPedido, Long idPropuesta) {
        OrderTermsProposal propuesta = propuestaTerminosPedidoRepository.findById(idPropuesta)
                .orElseThrow(() -> new ResourceNotFoundException("Propuesta no encontrada"));
        if (!propuesta.getPedido().getIdPedido().equals(idPedido)) {
            throw new ResourceNotFoundException("Propuesta no encontrada");
        }
        if (!OrderTermsProposal.PENDIENTE.equals(propuesta.getEstado())) {
            throw new BusinessRuleException("Esta propuesta ya fue resuelta");
        }
        return propuesta;
    }

    private User getOrderParty(Order pedido, Long idUsuario, String mensajeError) {
        Long idCliente = pedido.getUsuarioCliente().getIdUsuario();
        Long idCreador = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        if (idCliente.equals(idUsuario)) {
            return pedido.getUsuarioCliente();
        }
        if (idCreador.equals(idUsuario)) {
            return pedido.getServicio().getPerfil().getUsuario();
        }
        throw new BusinessRuleException(mensajeError);
    }

    private User getCounterparty(Order pedido, Long idUsuario) {
        boolean esCliente = pedido.getUsuarioCliente().getIdUsuario().equals(idUsuario);
        return esCliente ? pedido.getServicio().getPerfil().getUsuario() : pedido.getUsuarioCliente();
    }

    /**
     * Los términos quedan congelados apenas hay una firma: el contrato ya
     * renderiza precio/fecha en vivo desde el pedido (ver
     * ContractServiceImpl#generarContratoHtml), así que cambiarlos después
     * de que alguien firmó reescribiría en silencio lo que esa persona ya
     * aceptó.
     */
    private void validateContractUnsigned(Long idPedido) {
        contratoRepository.findByPedidoIdPedido(idPedido).ifPresent(contrato -> {
            if (contrato.getHashFirmaCreador() != null || contrato.getHashFirmaCliente() != null) {
                throw new BusinessRuleException(
                        "No se pueden modificar los términos: el contrato ya tiene al menos una firma");
            }
        });
    }

    private TermsProposalResponse mapProposal(OrderTermsProposal propuesta) {
        User propuestoPor = propuesta.getPropuestoPor();
        return TermsProposalResponse.builder()
                .idPropuesta(propuesta.getIdPropuesta())
                .idPedido(propuesta.getPedido().getIdPedido())
                .idUsuarioPropuso(propuestoPor.getIdUsuario())
                .nombrePropuso(propuestoPor.getNombres() + " " + propuestoPor.getApellidos())
                .precioPropuesto(propuesta.getPrecioPropuesto())
                .fechaEntregaPropuesta(propuesta.getFechaEntregaPropuesta())
                .estado(propuesta.getEstado())
                .fechaCreacion(propuesta.getFechaCreacion())
                .fechaResolucion(propuesta.getFechaResolucion())
                .build();
    }

    /**
     * Flujo que le corresponde al pedido: el que el creador asignó a su
     * servicio. Antes (RF-19) el flujo colgaba de la categoría del servicio;
     * eso acoplaba mal el catálogo (una categoría solo clasifica el rubro) con
     * la operación de pedidos, así que ahora cada servicio elige, entre los
     * flujos propios de su creador, cuál usar.
     *
     * <p>Si el servicio no tiene flujo asignado no se rechaza el pedido: cae
     * primero al flujo más antiguo del propio creador, y si el creador tampoco
     * tiene ninguno, al flujo por defecto global. La columna es nullable a
     * propósito para que un catálogo a medio configurar no impida vender.
     */
    private Workflow resolveServiceWorkflow(Offering servicio) {
        if (servicio.getFlujo() != null) {
            return servicio.getFlujo();
        }

        Long idCreador = servicio.getPerfil().getUsuario().getIdUsuario();
        return flujoTrabajoRepository.findFirstByCreadorIdUsuarioOrderByIdFlujoAsc(idCreador)
                .orElseGet(() -> {
                    log.warn("El servicio '{}' no tiene flujo asignado ni su creador tiene flujos propios; se usa el flujo por defecto",
                            servicio.getTituloServicio());
                    return getDefaultWorkflow();
                });
    }

    /**
     * Flujo de respaldo: el de menor id.
     *
     * <p>No se busca ningún flujo por nombre a propósito. Los nombres los fija
     * el seed de fixtures (`database/seed-medicion-referencia.sql`), no el
     * esquema, y acoplar el servicio a una cadena concreta lo rompería en
     * cualquier instalación que sembrase otros datos. Lo que sí se garantiza
     * frente al {@code findAll().get(0)} anterior es que la elección sea
     * determinista.
     */
    private Workflow getDefaultWorkflow() {
        return flujoTrabajoRepository.findFirstByOrderByIdFlujoAsc()
                .orElseThrow(() -> new BusinessRuleException(
                        "No hay flujos de trabajo configurados en el sistema"));
    }

    /**
     * REQ-F-016 ampliado: exige una respuesta no vacía por cada pregunta de
     * la plantilla, mismo criterio que ya aplicaba el frontend en el antiguo
     * flujo de envío manual (BriefingPedidoComponent#todasRespondidas). Se
     * llama antes de persistir el pedido para que un cuestionario incompleto
     * rechace la creación completa, no solo el briefing.
     */
    private void validateBriefingAnswersComplete(BriefingTemplate plantilla,
                                                      List<AnswerBriefingRequest.RespuestaItem> respuestas) {
        if (respuestas == null || respuestas.isEmpty()) {
            throw new BusinessRuleException(
                    "Este servicio tiene un cuestionario: responde todas sus preguntas para crear el pedido");
        }

        Set<Long> idsRespondidos = respuestas.stream()
                .filter(r -> r.getTextoRespuesta() != null && !r.getTextoRespuesta().isBlank())
                .map(AnswerBriefingRequest.RespuestaItem::getIdPregunta)
                .collect(Collectors.toSet());

        for (BriefingQuestion pregunta : plantilla.getPreguntas()) {
            if (!idsRespondidos.contains(pregunta.getIdPregunta())) {
                throw new BusinessRuleException(
                        "Falta responder la pregunta del cuestionario: \"" + pregunta.getTextoPregunta() + "\"");
            }
        }
    }

    /**
     * Crea el SentBriefing (ya completado) y sus BriefingAnswer en la
     * misma transacción que el pedido — reemplaza el antiguo camino en dos
     * pasos (BriefingServiceImpl.enviarBriefing + responderBriefing), que
     * dependía de que el creador lo disparara manualmente después.
     */
    private void recordBriefingCompleted(Order pedido, BriefingTemplate plantilla,
                                              List<AnswerBriefingRequest.RespuestaItem> respuestas) {
        SentBriefing enviado = SentBriefing.builder()
                .pedido(pedido)
                .plantilla(plantilla)
                .completado(true)
                .build();
        enviado = briefingEnviadoRepository.save(enviado);

        Map<Long, String> textoPorPregunta = respuestas.stream()
                .collect(Collectors.toMap(
                        AnswerBriefingRequest.RespuestaItem::getIdPregunta,
                        AnswerBriefingRequest.RespuestaItem::getTextoRespuesta,
                        (a, b) -> b));

        for (BriefingQuestion pregunta : plantilla.getPreguntas()) {
            BriefingAnswer respuesta = BriefingAnswer.builder()
                    .briefingEnviado(enviado)
                    .pregunta(pregunta)
                    .textoRespuesta(textoPorPregunta.get(pregunta.getIdPregunta()))
                    .build();
            briefingRespuestaRepository.save(respuesta);
        }
    }

    /**
     * Obtiene el detalle de un pedido.
     *
     * @param idPedido identificador del pedido
     * @param idUsuarioSolicitante identificador de quien consulta, para validar que sea parte del pedido o admin
     * @return el pedido solicitado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     */
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long idPedido, Long idUsuarioSolicitante) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado con ID: " + idPedido));
        // OBS-08 / H-02: evita el acceso indebido (IDOR) a pedidos ajenos.
        OrderOwnershipValidator.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);
        return mapToRespuesta(pedido);
    }

    /**
     * @param idCliente identificador del cliente
     * @return los pedidos realizados por ese cliente, resumidos
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listMyOrders(Long idCliente) {
        return pedidoRepository.findByUsuarioClienteIdUsuario(idCliente)
                .stream()
                .map(this::mapToResumido)
                .collect(Collectors.toList());
    }

    /**
     * @param idCreador identificador del creador
     * @return los pedidos recibidos por ese creador, resumidos
     */
    @Override
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listMyCommissions(Long idCreador) {
        return pedidoRepository.findByServicioPerfilUsuarioIdUsuario(idCreador)
                .stream()
                .map(this::mapToResumido)
                .collect(Collectors.toList());
    }

    /**
     * Exporta el listado de pedidos de un cliente.
     *
     * @param idCliente identificador del cliente
     * @param formato formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con el listado de pedidos del cliente
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el listado excede el tope de filas admitido por el formato
     */
    @Override
    @Transactional(readOnly = true)
    public GeneratedDocument exportMyOrders(Long idCliente, ReportFormat formato, String correoSolicitante) {
        return exportSummary(listMyOrders(idCliente), "Mis pedidos", "Pedidos como cliente",
                formato, correoSolicitante);
    }

    /**
     * Exporta el listado de comisiones (pedidos recibidos) de un creador,
     * opcionalmente acotado a un subconjunto de pedidos ya autorizados para él.
     *
     * @param idCreador identificador del creador
     * @param idsPedido si no es vacío, restringe la exportación a esos pedidos (los ajenos se ignoran)
     * @param formato formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con el listado de comisiones del creador
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el listado excede el tope de filas admitido por el formato
     */
    @Override
    @Transactional(readOnly = true)
    public GeneratedDocument exportMyCommissions(Long idCreador, List<Long> idsPedido, ReportFormat formato,
                                                     String correoSolicitante) {
        List<OrderSummaryResponse> comisiones = listMyCommissions(idCreador);
        if (idsPedido != null && !idsPedido.isEmpty()) {
            // 1.4: filtra sobre el propio listado del creador, así que un id
            // ajeno enviado por el cliente simplemente no matchea — no es una
            // vía de IDOR, solo se restringe el subconjunto ya autorizado.
            Set<Long> idsSolicitados = new HashSet<>(idsPedido);
            comisiones = comisiones.stream()
                    .filter(c -> idsSolicitados.contains(c.getIdPedido()))
                    .collect(Collectors.toList());
        }
        return exportSummary(comisiones, "Mis comisiones", "Pedidos como creador",
                formato, correoSolicitante);
    }

    private GeneratedDocument exportSummary(List<OrderSummaryResponse> filas, String titulo, String subtitulo,
                                               ReportFormat formato, String correoSolicitante) {
        if (filas.size() > formato.topeFilas()) {
            throw new BusinessRuleException(
                    "El listado tiene " + filas.size() + " pedidos, más de los " + formato.topeFilas()
                            + " que admite una exportación en " + formato + ".");
        }

        ReportModel<OrderSummaryResponse> modelo = ReportModel.<OrderSummaryResponse>builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
                .filtrosAplicados(Map.of())
                .columnas(List.of(
                        ReportColumn.entero("Id. pedido", OrderSummaryResponse::getIdPedido),
                        ReportColumn.texto("Offering", OrderSummaryResponse::getTituloServicio),
                        ReportColumn.texto("Etapa", OrderSummaryResponse::getEtapaActual),
                        ReportColumn.moneda("Precio pactado", OrderSummaryResponse::getPrecioPactado),
                        ReportColumn.fechaHora("Inicio", OrderSummaryResponse::getFechaInicio),
                        ReportColumn.fechaHora("Entrega estimada", OrderSummaryResponse::getFechaEntregaEstimada),
                        ReportColumn.texto("Creador", OrderSummaryResponse::getNombreCreador),
                        ReportColumn.texto("Cliente", OrderSummaryResponse::getNombreCliente)))
                .filas(filas)
                .generadoPor(correoSolicitante)
                .build();

        return servicioExportacion.exportar(modelo, formato);
    }

    @Override
    @Transactional
    // Las transiciones de flujo: incluye los intentos FALLIDOS, que
    // historial_estados_pedido (tabla de dominio) nunca registra.
    /**
     * Avanza el pedido a la siguiente etapa configurada de su flujo de trabajo.
     *
     * @param idPedido identificador del pedido
     * @param idCreador identificador del creador; debe ser dueño del servicio del pedido
     * @param peticion observación de la transición
     * @return el pedido ya en la nueva etapa
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien llama no es el creador del
     *         servicio, si la etapa actual exige un entregable que aún no se subió, si la etapa actual
     *         ya no está en la configuración del flujo, o si el pedido ya está en la etapa final
     */
    @Auditable(accion = "PEDIDO_AVANZAR_ETAPA", modulo = AuditModule.PEDIDOS,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{observacion: #peticion.observacion}")
    public OrderResponse advanceStage(Long idPedido, Long idCreador, AdvanceStageRequest peticion) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));

        // Verificar que el creador es el dueño del servicio del pedido
        Long idCreadorServicio = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        if (!idCreadorServicio.equals(idCreador)) {
            throw new BusinessRuleException("Solo el creador del servicio puede avanzar las etapas del pedido");
        }

        // Obtener etapa actual del historial
        OrderStatusHistory ultimoEstado = historialRepository
                .findTopByPedidoIdPedidoOrderByFechaTransicionDesc(idPedido)
                .orElseThrow(() -> new BusinessRuleException("Order sin estado inicial"));

        // Obtener configuracion de la etapa actual (orden + si exige entregable).
        // Si ya no está en la configuración del flujo (p. ej. se borró la
        // etapa), no hay un "siguiente" seguro que calcular — tratarlo como
        // orden 0 avanzaría el pedido a la primera etapa en vez de fallar.
        WorkflowStageConfig configActual = getCurrentConfig(pedido, ultimoEstado);
        if (configActual == null) {
            throw new BusinessRuleException(
                    "La etapa actual del pedido ('" + ultimoEstado.getEtapa().getNombreEtapa()
                            + "') ya no forma parte del flujo de trabajo configurado. Contacta a soporte.");
        }
        Integer ordenActual = configActual.getNumeroOrden();

        // La etapa que se abandona puede exigir que ya exista un entregable
        // subido para el pedido (p. ej. "En Producción" antes de pasar a
        // revisión del cliente); sin él, el creador no puede avanzar.
        if (Boolean.TRUE.equals(configActual.getRequiereEntregable())
                && !entregableFinalRepository.existsByPedidoIdPedido(idPedido)) {
            throw new BusinessRuleException(
                    "Debes subir el entregable antes de avanzar de la etapa '"
                            + configActual.getEtapa().getNombreEtapa() + "'");
        }

        // Mismo criterio que requiereEntregable: la etapa que se abandona
        // puede exigir un boceto ya subido para el pedido.
        if (Boolean.TRUE.equals(configActual.getRequiereBoceto())
                && !bocetoRepository.existsByPedidoIdPedido(idPedido)) {
            throw new BusinessRuleException(
                    "Debes subir un boceto antes de avanzar de la etapa '"
                            + configActual.getEtapa().getNombreEtapa() + "'");
        }

        // Obtener siguiente etapa del flujo configurado
        List<WorkflowStageConfig> siguientes = flujoEtapaConfigRepository
                .findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(
                        pedido.getFlujo().getIdFlujo(), ordenActual);

        if (siguientes.isEmpty()) {
            throw new BusinessRuleException("El pedido ya se encuentra en la etapa final");
        }

        WorkflowStageConfig siguienteConfig = siguientes.get(0);

        // Registrar transición (INMUTABLE)
        OrderStatusHistory nuevoEstado = OrderStatusHistory.builder()
                .pedido(pedido)
                .etapa(siguienteConfig.getEtapa())
                .observacion(peticion.getObservacion())
                .build();
        historialRepository.save(nuevoEstado);

        log.info("Order {} avanzó a etapa '{}' (orden {})",
                idPedido, siguienteConfig.getEtapa().getNombreEtapa(), siguienteConfig.getNumeroOrden());

        notificacionService.notify(pedido.getUsuarioCliente(), "PEDIDO_AVANCE",
                "Tu pedido ha avanzado a: " + siguienteConfig.getEtapa().getNombreEtapa());

        return mapToRespuesta(pedido);
    }

    /**
     * Obtiene el historial completo de transiciones de un pedido.
     *
     * @param idPedido identificador del pedido
     * @param idUsuarioSolicitante identificador de quien consulta, para validar que sea parte del pedido o admin
     * @return el historial de transiciones, en el orden en que ocurrieron
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     */
    @Override
    @Transactional(readOnly = true)
    public List<StatusHistoryResponse> getHistory(Long idPedido, Long idUsuarioSolicitante) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado con ID: " + idPedido));
        // Evita que cualquier autenticado lea el historial de un pedido ajeno.
        OrderOwnershipValidator.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

        return historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(idPedido)
                .stream()
                .map(this::mapHistory)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene el progreso del pedido dentro de su flujo de trabajo: etapa
     * actual, porcentaje de avance y si está bloqueado esperando un entregable.
     *
     * @param idPedido identificador del pedido
     * @param idUsuarioSolicitante identificador de quien consulta, para validar que sea parte del pedido o admin
     * @return el seguimiento del pedido
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     */
    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse getTracking(Long idPedido, Long idUsuarioSolicitante) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));
        // Evita que cualquier autenticado lea el seguimiento de un pedido ajeno.
        OrderOwnershipValidator.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

        List<WorkflowStageConfig> etapasConfig = flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(pedido.getFlujo().getIdFlujo());

        List<OrderStatusHistory> historial = historialRepository
                .findByPedidoIdPedidoOrderByFechaTransicionAsc(idPedido);

        OrderStatusHistory ultimoEstado = historial.isEmpty() ? null : historial.get(historial.size() - 1);

        Integer etapaActualOrden = 0;
        String etapaActualNombre = "Sin estado";
        boolean bloqueadoPorEntregable = false;
        boolean bloqueadoPorBoceto = false;
        if (ultimoEstado != null) {
            etapaActualNombre = ultimoEstado.getEtapa().getNombreEtapa();
            WorkflowStageConfig configActual = etapasConfig.stream()
                    .filter(c -> c.getEtapa().getIdEtapa().equals(ultimoEstado.getEtapa().getIdEtapa()))
                    .findFirst()
                    .orElse(null);
            if (configActual != null) {
                etapaActualOrden = configActual.getNumeroOrden();
                bloqueadoPorEntregable = Boolean.TRUE.equals(configActual.getRequiereEntregable())
                        && !entregableFinalRepository.existsByPedidoIdPedido(idPedido);
                bloqueadoPorBoceto = Boolean.TRUE.equals(configActual.getRequiereBoceto())
                        && !bocetoRepository.existsByPedidoIdPedido(idPedido);
            } else {
                // Vista de solo lectura: no se puede fallar, pero sí avisar.
                // El pedido quedó con una etapa que ya no está en la
                // configuración del flujo.
                log.warn("Order {} tiene como etapa actual '{}', que ya no está en la configuración del flujo {}",
                        idPedido, etapaActualNombre, pedido.getFlujo().getIdFlujo());
            }
        }

        int totalEtapas = etapasConfig.size();
        double porcentaje = totalEtapas > 0 ? ((double) etapaActualOrden / totalEtapas) * 100 : 0;

        return OrderTrackingResponse.builder()
                .idPedido(idPedido)
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .etapaActual(etapaActualNombre)
                .etapaActualOrden(etapaActualOrden)
                .totalEtapas(totalEtapas)
                .porcentajeProgreso(porcentaje)
                .fechaUltimaActualizacion(ultimoEstado != null ? ultimoEstado.getFechaTransicion() : null)
                .bloqueadoPorEntregable(bloqueadoPorEntregable)
                .bloqueadoPorBoceto(bloqueadoPorBoceto)
                .etapasDelFlujo(etapasConfig.stream().map(this::mapStageConfig).collect(Collectors.toList()))
                .historial(historial.stream().map(this::mapHistory).collect(Collectors.toList()))
                .build();
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    /**
     * Null cuando la etapa del último historial ya no está en la
     * configuración del flujo (p. ej. alguien la borró mientras el pedido
     * estaba detenido ahí). Nunca debe tratarse como "orden 0": eso haría
     * que advanceStage tome la primera etapa del flujo como "siguiente" y el
     * pedido retroceda en silencio.
     */
    private WorkflowStageConfig getCurrentConfig(Order pedido, OrderStatusHistory ultimoEstado) {
        return flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(pedido.getFlujo().getIdFlujo())
                .stream()
                .filter(c -> c.getEtapa().getIdEtapa().equals(ultimoEstado.getEtapa().getIdEtapa()))
                .findFirst()
                .orElse(null);
    }

    private String getCurrentStage(Long idPedido) {
        return historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(idPedido)
                .map(h -> h.getEtapa().getNombreEtapa())
                .orElse("Sin estado");
    }

    private OrderResponse mapToRespuesta(Order pedido) {
        List<StatusHistoryResponse> historial = historialRepository
                .findByPedidoIdPedidoOrderByFechaTransicionAsc(pedido.getIdPedido())
                .stream()
                .map(this::mapHistory)
                .collect(Collectors.toList());

        User creador = pedido.getServicio().getPerfil().getUsuario();

        return OrderResponse.builder()
                .idPedido(pedido.getIdPedido())
                .idServicio(pedido.getServicio().getIdServicio())
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .idCliente(pedido.getUsuarioCliente().getIdUsuario())
                .nombreCliente(pedido.getUsuarioCliente().getNombres() + " " + pedido.getUsuarioCliente().getApellidos())
                .idCreador(creador.getIdUsuario())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                // 1.2: dato ya presente en `historial` (ordenado ASC), evita repetir
                // la consulta que getCurrentStage(idPedido) haría por separado.
                .etapaActual(historial.isEmpty() ? "Sin estado" : historial.get(historial.size() - 1).getNombreEtapa())
                .precioPactado(pedido.getPrecioPactado())
                .fechaInicio(pedido.getFechaInicio())
                .fechaEntregaEstimada(pedido.getFechaEntregaEstimada())
                .nombreFlujo(pedido.getFlujo().getNombreFlujo())
                .historial(historial)
                .build();
    }

    private OrderSummaryResponse mapToResumido(Order pedido) {
        User creador = pedido.getServicio().getPerfil().getUsuario();

        return OrderSummaryResponse.builder()
                .idPedido(pedido.getIdPedido())
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .etapaActual(getCurrentStage(pedido.getIdPedido()))
                .precioPactado(pedido.getPrecioPactado())
                .fechaInicio(pedido.getFechaInicio())
                .fechaEntregaEstimada(pedido.getFechaEntregaEstimada())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                .nombreCliente(pedido.getUsuarioCliente().getNombres() + " " + pedido.getUsuarioCliente().getApellidos())
                .build();
    }

    private StatusHistoryResponse mapHistory(OrderStatusHistory h) {
        return StatusHistoryResponse.builder()
                .idHistorial(h.getIdHistorialEstado())
                .nombreEtapa(h.getEtapa().getNombreEtapa())
                .fechaTransicion(h.getFechaTransicion())
                .observacion(h.getObservacion())
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
                .requiereBoceto(config.getRequiereBoceto())
                .build();
    }
}
