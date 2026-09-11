package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionResponderBriefing;
import uteq.edu.ec.artisync.dto.peticion.pedido.AdvanceStageRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateOrderRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateTermsProposalRequest;
import uteq.edu.ec.artisync.dto.respuesta.pedido.*;
import uteq.edu.ec.artisync.entity.catalogo.Workflow;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingEnviado;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingPlantilla;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingPregunta;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingRespuesta;
import uteq.edu.ec.artisync.entity.pedido.*;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.WorkflowRepository;
import uteq.edu.ec.artisync.repository.catalogo.OfferingRepository;
import uteq.edu.ec.artisync.repository.comunicacion.BriefingEnviadoRepository;
import uteq.edu.ec.artisync.repository.comunicacion.BriefingRespuestaRepository;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.legal.FinalDeliverableRepository;
import uteq.edu.ec.artisync.repository.pedido.*;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;
import uteq.edu.ec.artisync.service.legal.IContractService;
import uteq.edu.ec.artisync.service.pedido.IOrderService;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
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
    private final OrderTermsProposalRepository propuestaTerminosPedidoRepository;
    private final NotificacionService notificacionService;
    private final ChatService chatService;
    private final IServicioExportacion servicioExportacion;
    private final IVerificacionServicio verificacionServicio;
    private final IContractService contratoServicio;
    private final BriefingEnviadoRepository briefingEnviadoRepository;
    private final BriefingRespuestaRepository briefingRespuestaRepository;

    @Override
    @Transactional
    @Auditable(accion = "PEDIDO_CREAR", modulo = AuditModule.PEDIDOS,
            entidad = "pedidos", idEntidad = "#resultado.idPedido",
            detalle = "{idServicio: #peticion.idServicio}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idCliente identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public OrderResponse crearPedido(Long idCliente, CreateOrderRequest peticion) {
        User cliente = usuarioRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("User cliente no encontrado"));

        if (!verificacionServicio.estaIdentidadVerificada(idCliente)) {
            throw new BusinessRuleException(
                    "Debes verificar tu identidad antes de crear un pedido. Sube tu documento de identidad desde tu perfil.");
        }

        Offering servicio = servicioRepository.findById(peticion.getIdServicio())
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado"));

        // Verificar que el cliente no sea el mismo creador del servicio
        if (servicio.getPerfil().getUsuario().getIdUsuario().equals(idCliente)) {
            throw new BusinessRuleException("No puedes crear un pedido para tu propio servicio");
        }

        Workflow flujo = resolverFlujoDelServicio(servicio);

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
        BriefingPlantilla plantillaBriefing = servicio.getBriefingPlantilla();
        if (plantillaBriefing != null) {
            validarRespuestasBriefingCompletas(plantillaBriefing, peticion.getRespuestasBriefing());
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
            registrarBriefingCompletado(pedido, plantillaBriefing, peticion.getRespuestasBriefing());
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
        // comprometerse con un contrato (ver proponerTerminos). Antes solo
        // se abría cuando ambas partes ya habían firmado.
        chatService.crearSala(pedido);

        return mapToRespuesta(pedido);
    }

    @Override
    @Transactional
    @Auditable(accion = "PEDIDO_PROPONER_TERMINOS", modulo = AuditModule.PEDIDOS,
            entidad = "pedidos", idEntidad = "#idPedido")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public TermsProposalResponse proponerTerminos(Long idPedido, Long idUsuario, CreateTermsProposalRequest peticion) {
        if (peticion.getPrecioPropuesto() == null && peticion.getFechaEntregaPropuesta() == null) {
            throw new BusinessRuleException("Debes indicar al menos un término a proponer");
        }

        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));

        User proponente = obtenerParteDelPedido(pedido, idUsuario,
                "No tienes permiso para proponer términos de este pedido");

        validarContratoSinFirmar(idPedido);

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

        User otraParte = obtenerContraparte(pedido, idUsuario);
        notificacionService.notificar(otraParte, "PEDIDO_PROPUESTA_TERMINOS_CREADA",
                "Te proponen nuevos términos para el pedido \"" + pedido.getServicio().getTituloServicio() + "\".");

        return mapPropuesta(propuesta);
    }

    @Override
    @Transactional
    @Auditable(accion = "PEDIDO_ACEPTAR_PROPUESTA_TERMINOS", modulo = AuditModule.PEDIDOS,
            entidad = "pedidos", idEntidad = "#idPedido")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idPropuesta identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public OrderResponse aceptarPropuestaTerminos(Long idPedido, Long idPropuesta, Long idUsuario) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));
        OrderTermsProposal propuesta = obtenerPropuestaPendienteDelPedido(idPedido, idPropuesta);

        if (propuesta.getPropuestoPor().getIdUsuario().equals(idUsuario)) {
            throw new BusinessRuleException("No puedes aceptar tu propia propuesta; debe hacerlo la otra parte");
        }
        obtenerParteDelPedido(pedido, idUsuario, "No tienes permiso para aceptar esta propuesta");

        validarContratoSinFirmar(idPedido);

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
            contratoServicio.generarContrato(idPedido, idUsuario);
        }

        notificacionService.notificar(propuesta.getPropuestoPor(),
                contratoRecienGenerado ? "PEDIDO_PROPUESTA_TERMINOS_ACEPTADA_CONTRATO_GENERADO" : "PEDIDO_PROPUESTA_TERMINOS_ACEPTADA",
                "Aceptaron tus términos propuestos para el pedido \"" + pedido.getServicio().getTituloServicio() + "\"."
                        + (contratoRecienGenerado ? " Se generó el contrato." : ""));

        return mapToRespuesta(pedido);
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idPropuesta identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public TermsProposalResponse rechazarPropuestaTerminos(Long idPedido, Long idPropuesta, Long idUsuario) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));
        OrderTermsProposal propuesta = obtenerPropuestaPendienteDelPedido(idPedido, idPropuesta);

        if (propuesta.getPropuestoPor().getIdUsuario().equals(idUsuario)) {
            throw new BusinessRuleException("No puedes rechazar tu propia propuesta; debe hacerlo la otra parte");
        }
        obtenerParteDelPedido(pedido, idUsuario, "No tienes permiso para rechazar esta propuesta");

        propuesta.setEstado(OrderTermsProposal.RECHAZADA);
        propuesta.setFechaResolucion(LocalDateTime.now());
        propuesta = propuestaTerminosPedidoRepository.save(propuesta);

        log.info("Order {} rechazó propuesta de términos {} (usuario {})", idPedido, idPropuesta, idUsuario);

        notificacionService.notificar(propuesta.getPropuestoPor(), "PEDIDO_PROPUESTA_TERMINOS_RECHAZADA",
                "Rechazaron tus términos propuestos para el pedido \"" + pedido.getServicio().getTituloServicio() + "\".");

        return mapPropuesta(propuesta);
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idPropuesta identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public TermsProposalResponse cancelarPropuestaTerminos(Long idPedido, Long idPropuesta, Long idUsuario) {
        OrderTermsProposal propuesta = obtenerPropuestaPendienteDelPedido(idPedido, idPropuesta);

        if (!propuesta.getPropuestoPor().getIdUsuario().equals(idUsuario)) {
            throw new BusinessRuleException("Solo quien propuso los términos puede cancelar la propuesta");
        }

        propuesta.setEstado(OrderTermsProposal.CANCELADA);
        propuesta.setFechaResolucion(LocalDateTime.now());
        propuesta = propuestaTerminosPedidoRepository.save(propuesta);

        log.info("Order {} canceló propuesta de términos {} (usuario {})", idPedido, idPropuesta, idUsuario);

        return mapPropuesta(propuesta);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuarioSolicitante identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public TermsProposalResponse obtenerPropuestaPendiente(Long idPedido, Long idUsuarioSolicitante) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));
        OrderOwnershipValidator.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

        OrderTermsProposal propuesta = propuestaTerminosPedidoRepository
                .findByPedidoIdPedidoAndEstado(idPedido, OrderTermsProposal.PENDIENTE)
                .orElseThrow(() -> new ResourceNotFoundException("No hay ninguna propuesta de términos pendiente para el pedido con ID: " + idPedido));

        return mapPropuesta(propuesta);
    }

    private OrderTermsProposal obtenerPropuestaPendienteDelPedido(Long idPedido, Long idPropuesta) {
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

    private User obtenerParteDelPedido(Order pedido, Long idUsuario, String mensajeError) {
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

    private User obtenerContraparte(Order pedido, Long idUsuario) {
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
    private void validarContratoSinFirmar(Long idPedido) {
        contratoRepository.findByPedidoIdPedido(idPedido).ifPresent(contrato -> {
            if (contrato.getHashFirmaCreador() != null || contrato.getHashFirmaCliente() != null) {
                throw new BusinessRuleException(
                        "No se pueden modificar los términos: el contrato ya tiene al menos una firma");
            }
        });
    }

    private TermsProposalResponse mapPropuesta(OrderTermsProposal propuesta) {
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
    private Workflow resolverFlujoDelServicio(Offering servicio) {
        if (servicio.getFlujo() != null) {
            return servicio.getFlujo();
        }

        Long idCreador = servicio.getPerfil().getUsuario().getIdUsuario();
        return flujoTrabajoRepository.findFirstByCreadorIdUsuarioOrderByIdFlujoAsc(idCreador)
                .orElseGet(() -> {
                    log.warn("El servicio '{}' no tiene flujo asignado ni su creador tiene flujos propios; se usa el flujo por defecto",
                            servicio.getTituloServicio());
                    return obtenerFlujoPorDefecto();
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
    private Workflow obtenerFlujoPorDefecto() {
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
    private void validarRespuestasBriefingCompletas(BriefingPlantilla plantilla,
                                                      List<PeticionResponderBriefing.RespuestaItem> respuestas) {
        if (respuestas == null || respuestas.isEmpty()) {
            throw new BusinessRuleException(
                    "Este servicio tiene un cuestionario: responde todas sus preguntas para crear el pedido");
        }

        Set<Long> idsRespondidos = respuestas.stream()
                .filter(r -> r.getTextoRespuesta() != null && !r.getTextoRespuesta().isBlank())
                .map(PeticionResponderBriefing.RespuestaItem::getIdPregunta)
                .collect(Collectors.toSet());

        for (BriefingPregunta pregunta : plantilla.getPreguntas()) {
            if (!idsRespondidos.contains(pregunta.getIdPregunta())) {
                throw new BusinessRuleException(
                        "Falta responder la pregunta del cuestionario: \"" + pregunta.getTextoPregunta() + "\"");
            }
        }
    }

    /**
     * Crea el BriefingEnviado (ya completado) y sus BriefingRespuesta en la
     * misma transacción que el pedido — reemplaza el antiguo camino en dos
     * pasos (BriefingServiceImpl.enviarBriefing + responderBriefing), que
     * dependía de que el creador lo disparara manualmente después.
     */
    private void registrarBriefingCompletado(Order pedido, BriefingPlantilla plantilla,
                                              List<PeticionResponderBriefing.RespuestaItem> respuestas) {
        BriefingEnviado enviado = BriefingEnviado.builder()
                .pedido(pedido)
                .plantilla(plantilla)
                .completado(true)
                .build();
        enviado = briefingEnviadoRepository.save(enviado);

        Map<Long, String> textoPorPregunta = respuestas.stream()
                .collect(Collectors.toMap(
                        PeticionResponderBriefing.RespuestaItem::getIdPregunta,
                        PeticionResponderBriefing.RespuestaItem::getTextoRespuesta,
                        (a, b) -> b));

        for (BriefingPregunta pregunta : plantilla.getPreguntas()) {
            BriefingRespuesta respuesta = BriefingRespuesta.builder()
                    .briefingEnviado(enviado)
                    .pregunta(pregunta)
                    .textoRespuesta(textoPorPregunta.get(pregunta.getIdPregunta()))
                    .build();
            briefingRespuestaRepository.save(respuesta);
        }
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuarioSolicitante identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public OrderResponse obtenerPedidoPorId(Long idPedido, Long idUsuarioSolicitante) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado con ID: " + idPedido));
        // OBS-08 / H-02: evita el acceso indebido (IDOR) a pedidos ajenos.
        OrderOwnershipValidator.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);
        return mapToRespuesta(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idCliente identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<OrderSummaryResponse> listarMisPedidos(Long idCliente) {
        return pedidoRepository.findByUsuarioClienteIdUsuario(idCliente)
                .stream()
                .map(this::mapToResumido)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idCreador identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<OrderSummaryResponse> listarMisComisiones(Long idCreador) {
        return pedidoRepository.findByServicioPerfilUsuarioIdUsuario(idCreador)
                .stream()
                .map(this::mapToResumido)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param idCliente identificador unico que referencia de manera univoca al registro
     * @param formato parametro requerido para la correcta ejecucion del procedimiento
     * @param correoSolicitante direccion de correo electronico del actor o usuario principal
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public DocumentoGenerado exportarMisPedidos(Long idCliente, FormatoReporte formato, String correoSolicitante) {
        return exportarResumen(listarMisPedidos(idCliente), "Mis pedidos", "Pedidos como cliente",
                formato, correoSolicitante);
    }

    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     * @param idCreador id del creador
     * @param idsPedido lista de ids de pedidos
     * @param formato formato de reporte
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     */
    @Override
    @Transactional(readOnly = true)
    public DocumentoGenerado exportarMisComisiones(Long idCreador, List<Long> idsPedido, FormatoReporte formato,
                                                     String correoSolicitante) {
        List<OrderSummaryResponse> comisiones = listarMisComisiones(idCreador);
        if (idsPedido != null && !idsPedido.isEmpty()) {
            // 1.4: filtra sobre el propio listado del creador, así que un id
            // ajeno enviado por el cliente simplemente no matchea — no es una
            // vía de IDOR, solo se restringe el subconjunto ya autorizado.
            Set<Long> idsSolicitados = new HashSet<>(idsPedido);
            comisiones = comisiones.stream()
                    .filter(c -> idsSolicitados.contains(c.getIdPedido()))
                    .collect(Collectors.toList());
        }
        return exportarResumen(comisiones, "Mis comisiones", "Pedidos como creador",
                formato, correoSolicitante);
    }

    private DocumentoGenerado exportarResumen(List<OrderSummaryResponse> filas, String titulo, String subtitulo,
                                               FormatoReporte formato, String correoSolicitante) {
        if (filas.size() > formato.topeFilas()) {
            throw new BusinessRuleException(
                    "El listado tiene " + filas.size() + " pedidos, más de los " + formato.topeFilas()
                            + " que admite una exportación en " + formato + ".");
        }

        ModeloReporte<OrderSummaryResponse> modelo = ModeloReporte.<OrderSummaryResponse>builder()
                .titulo(titulo)
                .subtitulo(subtitulo)
                .filtrosAplicados(Map.of())
                .columnas(List.of(
                        ColumnaReporte.entero("Id. pedido", OrderSummaryResponse::getIdPedido),
                        ColumnaReporte.texto("Offering", OrderSummaryResponse::getTituloServicio),
                        ColumnaReporte.texto("Etapa", OrderSummaryResponse::getEtapaActual),
                        ColumnaReporte.moneda("Precio pactado", OrderSummaryResponse::getPrecioPactado),
                        ColumnaReporte.fechaHora("Inicio", OrderSummaryResponse::getFechaInicio),
                        ColumnaReporte.fechaHora("Entrega estimada", OrderSummaryResponse::getFechaEntregaEstimada),
                        ColumnaReporte.texto("Creador", OrderSummaryResponse::getNombreCreador),
                        ColumnaReporte.texto("Cliente", OrderSummaryResponse::getNombreCliente)))
                .filas(filas)
                .generadoPor(correoSolicitante)
                .build();

        return servicioExportacion.exportar(modelo, formato);
    }

    @Override
    @Transactional
    // Las transiciones de flujo: incluye los intentos FALLIDOS, que
    // historial_estados_pedido (tabla de dominio) nunca registra.
    @Auditable(accion = "PEDIDO_AVANZAR_ETAPA", modulo = AuditModule.PEDIDOS,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{observacion: #peticion.observacion}")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idCreador identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public OrderResponse avanzarEtapa(Long idPedido, Long idCreador, AdvanceStageRequest peticion) {
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
        WorkflowStageConfig configActual = obtenerConfigActual(pedido, ultimoEstado);
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

        notificacionService.notificar(pedido.getUsuarioCliente(), "PEDIDO_AVANCE",
                "Tu pedido ha avanzado a: " + siguienteConfig.getEtapa().getNombreEtapa());

        return mapToRespuesta(pedido);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuarioSolicitante identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<StatusHistoryResponse> obtenerHistorial(Long idPedido, Long idUsuarioSolicitante) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado con ID: " + idPedido));
        // Evita que cualquier autenticado lea el historial de un pedido ajeno.
        OrderOwnershipValidator.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

        return historialRepository.findByPedidoIdPedidoOrderByFechaTransicionAsc(idPedido)
                .stream()
                .map(this::mapHistorial)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuarioSolicitante identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public OrderTrackingResponse obtenerSeguimiento(Long idPedido, Long idUsuarioSolicitante) {
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
                .etapasDelFlujo(etapasConfig.stream().map(this::mapEtapaConfig).collect(Collectors.toList()))
                .historial(historial.stream().map(this::mapHistorial).collect(Collectors.toList()))
                .build();
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    /**
     * Null cuando la etapa del último historial ya no está en la
     * configuración del flujo (p. ej. alguien la borró mientras el pedido
     * estaba detenido ahí). Nunca debe tratarse como "orden 0": eso haría
     * que avanzarEtapa tome la primera etapa del flujo como "siguiente" y el
     * pedido retroceda en silencio.
     */
    private WorkflowStageConfig obtenerConfigActual(Order pedido, OrderStatusHistory ultimoEstado) {
        return flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(pedido.getFlujo().getIdFlujo())
                .stream()
                .filter(c -> c.getEtapa().getIdEtapa().equals(ultimoEstado.getEtapa().getIdEtapa()))
                .findFirst()
                .orElse(null);
    }

    private String obtenerEtapaActual(Long idPedido) {
        return historialRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(idPedido)
                .map(h -> h.getEtapa().getNombreEtapa())
                .orElse("Sin estado");
    }

    private OrderResponse mapToRespuesta(Order pedido) {
        List<StatusHistoryResponse> historial = historialRepository
                .findByPedidoIdPedidoOrderByFechaTransicionAsc(pedido.getIdPedido())
                .stream()
                .map(this::mapHistorial)
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
                // la consulta que obtenerEtapaActual(idPedido) haría por separado.
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
                .etapaActual(obtenerEtapaActual(pedido.getIdPedido()))
                .precioPactado(pedido.getPrecioPactado())
                .fechaInicio(pedido.getFechaInicio())
                .fechaEntregaEstimada(pedido.getFechaEntregaEstimada())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                .nombreCliente(pedido.getUsuarioCliente().getNombres() + " " + pedido.getUsuarioCliente().getApellidos())
                .build();
    }

    private StatusHistoryResponse mapHistorial(OrderStatusHistory h) {
        return StatusHistoryResponse.builder()
                .idHistorial(h.getIdHistorialEstado())
                .nombreEtapa(h.getEtapa().getNombreEtapa())
                .fechaTransicion(h.getFechaTransicion())
                .observacion(h.getObservacion())
                .build();
    }

    private StageConfigResponse mapEtapaConfig(WorkflowStageConfig config) {
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
