package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateRevisionTicketRequest;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RevisionTicketResponse;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.pedido.RevisionTicket;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.pedido.RejectionReasonRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.pedido.RevisionTicketRepository;
import uteq.edu.ec.artisync.service.legal.IRevisionTicketPaymentService;
import uteq.edu.ec.artisync.service.pedido.IRevisionTicketService;
import uteq.edu.ec.artisync.util.OrderOwnershipValidator;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevisionTicketServiceImpl implements IRevisionTicketService {

    private final RevisionTicketRepository ticketRevisionRepository;
    private final OrderRepository pedidoRepository;
    private final RejectionReasonRepository motivoRechazoRepository;
    private final ContractRepository contratoRepository;
    private final IRevisionTicketPaymentService pagoTicketRevisionServicio;

    @Override
    @Transactional
    @Auditable(accion = "TICKET_CREAR", modulo = AuditModule.PEDIDOS,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{idMotivo: #peticion.idMotivo}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     * @param idPedido id del pedido
     * @param idCliente id del cliente
     * @param peticion peticion
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     */
    public RevisionTicketResponse createRevisionTicket(Long idPedido, Long idCliente,
                                                        CreateRevisionTicketRequest peticion) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));

        // Verificar que el usuario es el cliente del pedido
        if (!pedido.getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new BusinessRuleException("Solo el cliente del pedido puede crear tickets de revision");
        }

        var motivo = motivoRechazoRepository.findById(peticion.getIdMotivo())
                .orElseThrow(() -> new ResourceNotFoundException("Motivo de rechazo no encontrado"));

        RevisionTicket ticket = RevisionTicket.builder()
                .pedido(pedido)
                .motivo(motivo)
                .descripcionCliente(peticion.getDescripcionCliente())
                .estadoTicket("Abierto")
                .build();

        // Verificar si supera el límite de revisiones del contrato
        var contratoOpt = contratoRepository.findByPedidoIdPedido(idPedido);
        if (contratoOpt.isPresent()) {
            long revisionesActuales = ticketRevisionRepository.countByPedidoIdPedido(idPedido);
            int limiteRevisiones = contratoOpt.get().getLimiteRevisiones();

            if (revisionesActuales >= limiteRevisiones && limiteRevisiones > 0) {
                // Supera el límite → marcar costo adicional
                var cargoExtra = pedido.getServicio().getCargoRevisionAdicional();
                ticket.setCostoAdicionalGenerado(cargoExtra);
                log.info("Ticket de revision para pedido {} supera el limite ({}/{}). Cargo adicional: {}",
                        idPedido, revisionesActuales, limiteRevisiones, cargoExtra);
            }
        }

        ticket = ticketRevisionRepository.save(ticket);
        log.info("Ticket de revision {} creado para pedido {}", ticket.getIdTicket(), idPedido);

        // REQ-F-022b: el enlace de pago se genera automaticamente al crear el
        // ticket (asi lo exige el SRS: "genera un nuevo enlace de pago"), no en
        // un endpoint aparte que el cliente deba pedir.
        if (ticket.getCostoAdicionalGenerado().signum() > 0) {
            pagoTicketRevisionServicio.crearOrdenPago(ticket);
        }

        return mapToRespuesta(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuarioSolicitante identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RevisionTicketResponse> listTicketsByOrder(Long idPedido, Long idUsuarioSolicitante) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado con ID: " + idPedido));
        // OBS-08 / H-02: evita el acceso indebido (IDOR) a tickets de un pedido ajeno.
        OrderOwnershipValidator.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

        return ticketRevisionRepository.findByPedidoIdPedidoOrderByIdTicketDesc(idPedido)
                .stream()
                .map(this::mapToRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "TICKET_CAMBIAR_ESTADO", modulo = AuditModule.PEDIDOS,
            entidad = "tickets_revision", idEntidad = "#idTicket",
            detalle = "{nuevoEstado: #nuevoEstado}")
    /**
     * Aplica una transicion de estado especifica sobre el ciclo de vida del recurso.
     *
     * @param idTicket identificador unico que referencia de manera univoca al registro
     * @param idCreador identificador unico que referencia de manera univoca al registro
     * @param nuevoEstado parametro requerido para la correcta ejecucion del procedimiento
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RevisionTicketResponse changeTicketStatus(Long idTicket, Long idCreador, String nuevoEstado) {
        RevisionTicket ticket = ticketRevisionRepository.findById(idTicket)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket de revision no encontrado"));

        // El creador del servicio siempre puede resolver sus propios tickets.
        // Además, el controlador autoriza aquí a SOPORTE (TICKET_RESOLVER) y a
        // quien tenga PEDIDO_GESTIONAR/ADMIN -- este chequeo exigía SIEMPRE
        // ser el creador, así que soporte/admin recibía 422 pese a que el
        // @PreAuthorize les daba paso: la funcionalidad de soporte estaba
        // rota en la práctica.
        Long idCreadorServicio = ticket.getPedido().getServicio().getPerfil().getUsuario().getIdUsuario();
        if (!idCreadorServicio.equals(idCreador) && !tienePermisoDeSoporteOAdmin()) {
            throw new AccessDeniedException("No tienes permisos para cambiar el estado de este ticket");
        }

        ticket.setEstadoTicket(nuevoEstado);
        ticket = ticketRevisionRepository.save(ticket);

        log.info("Ticket {} cambio a estado '{}'", idTicket, nuevoEstado);
        return mapToRespuesta(ticket);
    }

    /** Mismos roles que el @PreAuthorize del endpoint, aparte del creador del servicio. */
    private boolean tienePermisoDeSoporteOAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a ->
                a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("TICKET_RESOLVER")
                        || a.getAuthority().equals("PEDIDO_GESTIONAR"));
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private RevisionTicketResponse mapToRespuesta(RevisionTicket ticket) {
        return RevisionTicketResponse.builder()
                .idTicket(ticket.getIdTicket())
                .idPedido(ticket.getPedido().getIdPedido())
                .descripcionMotivo(ticket.getMotivo().getDescripcionMotivo())
                .descripcionCliente(ticket.getDescripcionCliente())
                .estadoTicket(ticket.getEstadoTicket())
                .costoAdicionalGenerado(ticket.getCostoAdicionalGenerado())
                .urlPagoAdicional(pagoTicketRevisionServicio.obtenerUrlPagoPendiente(ticket.getIdTicket()))
                .build();
    }
}
