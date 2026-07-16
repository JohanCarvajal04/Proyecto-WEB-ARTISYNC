package uteq.edu.ec.artisync.controller.pedido;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearTicketRevision;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaTicketRevision;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.ITicketRevisionServicio;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TicketRevisionControlador {

    private final ITicketRevisionServicio ticketRevisionServicio;

    @PostMapping("/pedidos/{idPedido}/tickets-revision")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public ResponseEntity<RespuestaTicketRevision> crearTicket(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PeticionCrearTicketRevision peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketRevisionServicio.crearTicketRevision(idPedido, userDetails.getIdUsuario(), peticion));
    }

    @GetMapping("/pedidos/{idPedido}/tickets-revision")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RespuestaTicketRevision>> listarTickets(@PathVariable Long idPedido) {
        return ResponseEntity.ok(ticketRevisionServicio.listarTicketsPorPedido(idPedido));
    }

    @PutMapping("/tickets-revision/{idTicket}/estado")
    @PreAuthorize("hasAnyRole('CREADOR', 'ADMIN')")
    public ResponseEntity<RespuestaTicketRevision> cambiarEstado(
            @PathVariable Long idTicket,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String nuevoEstado) {
        return ResponseEntity.ok(
                ticketRevisionServicio.cambiarEstadoTicket(idTicket, userDetails.getIdUsuario(), nuevoEstado));
    }
}
