package uteq.edu.ec.artisync.controller.pedido;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateRevisionTicketRequest;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RevisionTicketResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.IRevisionTicketService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevisionTicketControllerTest {

    @Mock
    private IRevisionTicketService ticketRevisionServicio;

    @InjectMocks
    private RevisionTicketController controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void crearTicket_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        CreateRevisionTicketRequest peticion = new CreateRevisionTicketRequest();
        RevisionTicketResponse respuesta = new RevisionTicketResponse();
        when(ticketRevisionServicio.createRevisionTicket(10L, 1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RevisionTicketResponse> res = controlador.createTicket(10L, user, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarTickets_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<RevisionTicketResponse> lista = Collections.emptyList();
        when(ticketRevisionServicio.listTicketsByOrder(10L, 1L)).thenReturn(lista);

        ResponseEntity<List<RevisionTicketResponse>> res = controlador.listTickets(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void cambiarEstado_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RevisionTicketResponse respuesta = new RevisionTicketResponse();
        when(ticketRevisionServicio.changeTicketStatus(10L, 1L, "ABIERTO")).thenReturn(respuesta);

        ResponseEntity<RevisionTicketResponse> res = controlador.changeStatus(10L, user, "ABIERTO");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }
}
