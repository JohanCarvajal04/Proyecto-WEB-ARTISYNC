package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearTicketRevision;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaTicketRevision;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.TicketRevision;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.pedido.MotivoRechazoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.pedido.TicketRevisionRepository;
import uteq.edu.ec.artisync.service.legal.IPagoTicketRevisionServicio;
import uteq.edu.ec.artisync.service.pedido.ITicketRevisionServicio;
import uteq.edu.ec.artisync.util.ValidadorPertenenciaPedido;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketRevisionServicioImpl implements ITicketRevisionServicio {

    private final TicketRevisionRepository ticketRevisionRepository;
    private final PedidoRepository pedidoRepository;
    private final MotivoRechazoRepository motivoRechazoRepository;
    private final ContratoRepository contratoRepository;
    private final IPagoTicketRevisionServicio pagoTicketRevisionServicio;

    @Override
    @Transactional
    @Auditable(accion = "TICKET_CREAR", modulo = ModuloAuditoria.PEDIDOS,
            entidad = "pedidos", idEntidad = "#idPedido",
            detalle = "{idMotivo: #peticion.idMotivo}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     * @param idPedido id del pedido
     * @param idCliente id del cliente
     * @param peticion peticion
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     */
    public RespuestaTicketRevision crearTicketRevision(Long idPedido, Long idCliente,
                                                        PeticionCrearTicketRevision peticion) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));

        // Verificar que el usuario es el cliente del pedido
        if (!pedido.getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new ExcepcionReglaNegocio("Solo el cliente del pedido puede crear tickets de revision");
        }

        var motivo = motivoRechazoRepository.findById(peticion.getIdMotivo())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Motivo de rechazo no encontrado"));

        TicketRevision ticket = TicketRevision.builder()
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
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaTicketRevision> listarTicketsPorPedido(Long idPedido, Long idUsuarioSolicitante) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado con ID: " + idPedido));
        // OBS-08 / H-02: evita el acceso indebido (IDOR) a tickets de un pedido ajeno.
        ValidadorPertenenciaPedido.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

        return ticketRevisionRepository.findByPedidoIdPedidoOrderByIdTicketDesc(idPedido)
                .stream()
                .map(this::mapToRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "TICKET_CAMBIAR_ESTADO", modulo = ModuloAuditoria.PEDIDOS,
            entidad = "tickets_revision", idEntidad = "#idTicket",
            detalle = "{nuevoEstado: #nuevoEstado}")
    /**
     * Aplica una transicion de estado especifica sobre el ciclo de vida del recurso.
     *
     * @param idTicket identificador unico que referencia de manera univoca al registro
     * @param idCreador identificador unico que referencia de manera univoca al registro
     * @param nuevoEstado parametro requerido para la correcta ejecucion del procedimiento
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaTicketRevision cambiarEstadoTicket(Long idTicket, Long idCreador, String nuevoEstado) {
        TicketRevision ticket = ticketRevisionRepository.findById(idTicket)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Ticket de revision no encontrado"));

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

    private RespuestaTicketRevision mapToRespuesta(TicketRevision ticket) {
        return RespuestaTicketRevision.builder()
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
