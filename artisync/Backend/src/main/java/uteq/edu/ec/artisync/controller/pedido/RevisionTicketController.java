package uteq.edu.ec.artisync.controller.pedido;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateRevisionTicketRequest;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RevisionTicketResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.IRevisionTicketService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RevisionTicketController {

    private final IRevisionTicketService ticketRevisionServicio;

    /**
     * Crea un ticket de revisión (rechazo) para el entregable actual de un pedido.
     *
     * @param idPedido identificador del pedido
     * @param userDetails usuario autenticado que crea el ticket
     * @param peticion datos del ticket de revisión, incluyendo el motivo de rechazo
     * @return el ticket de revisión creado, con estado 201
     * @throws ResourceNotFoundException si el pedido o el motivo de rechazo no existen
     * @throws BusinessRuleException si el usuario no es el cliente del pedido
     */
    @PostMapping("/pedidos/{idPedido}/tickets-revision")
    @PreAuthorize("hasAuthority('TICKET_REVISAR') or hasRole('ADMIN')")
    public ResponseEntity<RevisionTicketResponse> crearTicket(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateRevisionTicketRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketRevisionServicio.crearTicketRevision(idPedido, userDetails.getIdUsuario(), peticion));
    }

    /**
     * Lista los tickets de revisión asociados a un pedido.
     *
     * @param idPedido identificador del pedido
     * @param userDetails usuario autenticado que consulta los tickets
     * @return listado de tickets de revisión del pedido
     * @throws ResourceNotFoundException si el pedido no existe
     */
    @GetMapping("/pedidos/{idPedido}/tickets-revision")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RevisionTicketResponse>> listarTickets(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ticketRevisionServicio.listarTicketsPorPedido(idPedido, userDetails.getIdUsuario()));
    }

    /**
     * Cambia el estado de un ticket de revisión.
     *
     * @param idTicket identificador del ticket de revisión
     * @param userDetails usuario autenticado que solicita el cambio de estado
     * @param nuevoEstado nuevo estado a asignar al ticket
     * @return el ticket de revisión con su estado actualizado
     * @throws ResourceNotFoundException si el ticket no existe
     */
    @PutMapping("/tickets-revision/{idTicket}/estado")
    @PreAuthorize("hasAuthority('TICKET_RESOLVER') or hasAuthority('PEDIDO_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<RevisionTicketResponse> cambiarEstado(
            @PathVariable Long idTicket,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String nuevoEstado) {
        return ResponseEntity.ok(
                ticketRevisionServicio.cambiarEstadoTicket(idTicket, userDetails.getIdUsuario(), nuevoEstado));
    }
}
