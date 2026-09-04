package uteq.edu.ec.artisync.controller.pedido;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearTicketRevision;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaTicketRevision;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.ITicketRevisionServicio;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketRevisionControladorTest {

    @Mock
    private ITicketRevisionServicio ticketRevisionServicio;

    @InjectMocks
    private TicketRevisionControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void crearTicket_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        PeticionCrearTicketRevision peticion = new PeticionCrearTicketRevision();
        RespuestaTicketRevision respuesta = new RespuestaTicketRevision();
        when(ticketRevisionServicio.crearTicketRevision(10L, 1L, peticion)).thenReturn(respuesta);

        ResponseEntity<RespuestaTicketRevision> res = controlador.crearTicket(10L, user, peticion);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarTickets_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        List<RespuestaTicketRevision> lista = Collections.emptyList();
        when(ticketRevisionServicio.listarTicketsPorPedido(10L, 1L)).thenReturn(lista);

        ResponseEntity<List<RespuestaTicketRevision>> res = controlador.listarTickets(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void cambiarEstado_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaTicketRevision respuesta = new RespuestaTicketRevision();
        when(ticketRevisionServicio.cambiarEstadoTicket(10L, 1L, "ABIERTO")).thenReturn(respuesta);

        ResponseEntity<RespuestaTicketRevision> res = controlador.cambiarEstado(10L, user, "ABIERTO");
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }
}
