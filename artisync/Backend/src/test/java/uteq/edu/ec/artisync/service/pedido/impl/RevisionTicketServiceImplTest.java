package uteq.edu.ec.artisync.service.pedido.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateRevisionTicketRequest;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RevisionTicketResponse;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.entity.pedido.RejectionReason;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.pedido.RevisionTicket;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.pedido.RejectionReasonRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.pedido.RevisionTicketRepository;
import uteq.edu.ec.artisync.service.legal.IRevisionTicketPaymentService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RevisionTicketServiceImplTest {

    @Mock private RevisionTicketRepository ticketRevisionRepository;
    @Mock private OrderRepository pedidoRepository;
    @Mock private RejectionReasonRepository motivoRechazoRepository;
    @Mock private ContractRepository contratoRepository;
    @Mock private IRevisionTicketPaymentService pagoTicketRevisionServicio;

    @InjectMocks
    private RevisionTicketServiceImpl ticketRevisionServicio;

    private User cliente;
    private User creador;
    private Order pedido;
    private RejectionReason motivo;

    @BeforeEach
    void setUp() {
        cliente = User.builder().idUsuario(1L).nombres("Cliente").apellidos("Uno").correo("cliente@test.com").build();
        creador = User.builder().idUsuario(2L).nombres("Creador").apellidos("Uno").correo("creador@test.com").build();
        PerfilCreador perfil = PerfilCreador.builder().idPerfil(1L).usuario(creador).build();
        Offering servicio = Offering.builder().idServicio(1L).perfil(perfil).cargoRevisionAdicional(new BigDecimal("5.00")).build();
        pedido = Order.builder().idPedido(1L).usuarioCliente(cliente).servicio(servicio).precioPactado(BigDecimal.TEN).build();
        motivo = RejectionReason.builder().idMotivo(1L).descripcionMotivo("Calidad insuficiente").build();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("crearTicketRevision guarda el ticket cuando lo crea el cliente del pedido")
    void crearTicketRevision_guarda() {
        CreateRevisionTicketRequest peticion = CreateRevisionTicketRequest.builder().idMotivo(1L).descripcionCliente("No cumple").build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(motivoRechazoRepository.findById(1L)).willReturn(Optional.of(motivo));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.empty());
        given(ticketRevisionRepository.save(any(RevisionTicket.class))).willAnswer(inv -> inv.getArgument(0));

        RevisionTicketResponse respuesta = ticketRevisionServicio.crearTicketRevision(1L, 1L, peticion);

        assertThat(respuesta.getDescripcionCliente()).isEqualTo("No cumple");
        assertThat(respuesta.getEstadoTicket()).isEqualTo("Abierto");
    }

    @Test
    @DisplayName("crearTicketRevision rechaza a un usuario que no es el cliente del pedido")
    void crearTicketRevision_rechazaNoCliente() {
        CreateRevisionTicketRequest peticion = CreateRevisionTicketRequest.builder().idMotivo(1L).descripcionCliente("No cumple").build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> ticketRevisionServicio.crearTicketRevision(1L, 99L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("crearTicketRevision lanza recurso no encontrado si el pedido no existe")
    void crearTicketRevision_pedidoInexistente() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketRevisionServicio.crearTicketRevision(1L, 1L, CreateRevisionTicketRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crearTicketRevision lanza recurso no encontrado si el motivo no existe")
    void crearTicketRevision_motivoInexistente() {
        CreateRevisionTicketRequest peticion = CreateRevisionTicketRequest.builder().idMotivo(99L).descripcionCliente("No cumple").build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(motivoRechazoRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketRevisionServicio.crearTicketRevision(1L, 1L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crearTicketRevision marca cargo adicional cuando supera el limite de revisiones del contrato")
    void crearTicketRevision_marcaCargoAdicional() {
        CreateRevisionTicketRequest peticion = CreateRevisionTicketRequest.builder().idMotivo(1L).descripcionCliente("No cumple").build();
        Contract contrato = Contract.builder().limiteRevisiones(1).build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(motivoRechazoRepository.findById(1L)).willReturn(Optional.of(motivo));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contrato));
        given(ticketRevisionRepository.countByPedidoIdPedido(1L)).willReturn(1L);
        given(ticketRevisionRepository.save(any(RevisionTicket.class))).willAnswer(inv -> inv.getArgument(0));

        RevisionTicketResponse respuesta = ticketRevisionServicio.crearTicketRevision(1L, 1L, peticion);

        assertThat(respuesta.getCostoAdicionalGenerado()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("crearTicketRevision no marca cargo adicional si aun no se alcanza el limite")
    void crearTicketRevision_sinCargoAdicionalDentroDelLimite() {
        CreateRevisionTicketRequest peticion = CreateRevisionTicketRequest.builder().idMotivo(1L).descripcionCliente("No cumple").build();
        Contract contrato = Contract.builder().limiteRevisiones(3).build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(motivoRechazoRepository.findById(1L)).willReturn(Optional.of(motivo));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contrato));
        given(ticketRevisionRepository.countByPedidoIdPedido(1L)).willReturn(1L);
        given(ticketRevisionRepository.save(any(RevisionTicket.class))).willAnswer(inv -> inv.getArgument(0));

        RevisionTicketResponse respuesta = ticketRevisionServicio.crearTicketRevision(1L, 1L, peticion);

        assertThat(respuesta.getCostoAdicionalGenerado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /** REQ-F-022b: el enlace de pago se genera automaticamente, no en un endpoint aparte. */
    @Test
    @DisplayName("crearTicketRevision dispara la creacion de la orden de pago cuando supera el limite")
    void crearTicketRevision_superaLimite_disparaCreacionDeOrdenDePago() {
        CreateRevisionTicketRequest peticion = CreateRevisionTicketRequest.builder().idMotivo(1L).descripcionCliente("No cumple").build();
        Contract contrato = Contract.builder().limiteRevisiones(1).build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(motivoRechazoRepository.findById(1L)).willReturn(Optional.of(motivo));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contrato));
        given(ticketRevisionRepository.countByPedidoIdPedido(1L)).willReturn(1L);
        given(ticketRevisionRepository.save(any(RevisionTicket.class))).willAnswer(inv -> inv.getArgument(0));

        ticketRevisionServicio.crearTicketRevision(1L, 1L, peticion);

        verify(pagoTicketRevisionServicio).crearOrdenPago(any(RevisionTicket.class));
    }

    @Test
    @DisplayName("crearTicketRevision no dispara ninguna orden de pago si aun no se alcanza el limite")
    void crearTicketRevision_noSuperaLimite_noDisparaCreacionDeOrdenDePago() {
        CreateRevisionTicketRequest peticion = CreateRevisionTicketRequest.builder().idMotivo(1L).descripcionCliente("No cumple").build();
        Contract contrato = Contract.builder().limiteRevisiones(3).build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(motivoRechazoRepository.findById(1L)).willReturn(Optional.of(motivo));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contrato));
        given(ticketRevisionRepository.countByPedidoIdPedido(1L)).willReturn(1L);
        given(ticketRevisionRepository.save(any(RevisionTicket.class))).willAnswer(inv -> inv.getArgument(0));

        ticketRevisionServicio.crearTicketRevision(1L, 1L, peticion);

        verify(pagoTicketRevisionServicio, never()).crearOrdenPago(any());
    }

    @Test
    @DisplayName("listarTicketsPorPedido devuelve los tickets cuando el solicitante es el cliente")
    void listarTicketsPorPedido_clientePuedeVer() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(ticketRevisionRepository.findByPedidoIdPedidoOrderByIdTicketDesc(1L))
                .willReturn(List.of(RevisionTicket.builder().idTicket(1L).pedido(pedido).motivo(motivo).descripcionCliente("x").build()));

        assertThat(ticketRevisionServicio.listarTicketsPorPedido(1L, 1L)).hasSize(1);
    }

    @Test
    @DisplayName("listarTicketsPorPedido devuelve los tickets cuando el solicitante es el creador")
    void listarTicketsPorPedido_creadorPuedeVer() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(ticketRevisionRepository.findByPedidoIdPedidoOrderByIdTicketDesc(1L)).willReturn(List.of());

        assertThat(ticketRevisionServicio.listarTicketsPorPedido(1L, 2L)).isEmpty();
    }

    @Test
    @DisplayName("listarTicketsPorPedido permite a un ADMIN autenticado consultar tickets ajenos")
    void listarTicketsPorPedido_adminPuedeVer() {
        autenticarComo("admin@test.com", "ROLE_ADMIN");
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(ticketRevisionRepository.findByPedidoIdPedidoOrderByIdTicketDesc(1L)).willReturn(List.of());

        assertThat(ticketRevisionServicio.listarTicketsPorPedido(1L, 99L)).isEmpty();
    }

    @Test
    @DisplayName("listarTicketsPorPedido rechaza a un usuario ajeno sin rol admin")
    void listarTicketsPorPedido_rechazaAjeno() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> ticketRevisionServicio.listarTicketsPorPedido(1L, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("listarTicketsPorPedido lanza recurso no encontrado si el pedido no existe")
    void listarTicketsPorPedido_pedidoInexistente() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketRevisionServicio.listarTicketsPorPedido(1L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("cambiarEstadoTicket actualiza el estado cuando lo hace el creador del servicio")
    void cambiarEstadoTicket_actualiza() {
        RevisionTicket ticket = RevisionTicket.builder().idTicket(1L).pedido(pedido).motivo(motivo).descripcionCliente("x").estadoTicket("Abierto").build();
        given(ticketRevisionRepository.findById(1L)).willReturn(Optional.of(ticket));
        given(ticketRevisionRepository.save(any(RevisionTicket.class))).willAnswer(inv -> inv.getArgument(0));

        RevisionTicketResponse respuesta = ticketRevisionServicio.cambiarEstadoTicket(1L, 2L, "Resuelto");

        assertThat(respuesta.getEstadoTicket()).isEqualTo("Resuelto");
    }

    @Test
    @DisplayName("cambiarEstadoTicket rechaza a un usuario que no es el creador del servicio ni tiene rol de soporte/admin")
    void cambiarEstadoTicket_rechazaNoCreador() {
        RevisionTicket ticket = RevisionTicket.builder().idTicket(1L).pedido(pedido).motivo(motivo).descripcionCliente("x").build();
        given(ticketRevisionRepository.findById(1L)).willReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketRevisionServicio.cambiarEstadoTicket(1L, 99L, "Resuelto"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("cambiarEstadoTicket permite a SOPORTE (TICKET_RESOLVER) aunque no sea el creador del servicio")
    void cambiarEstadoTicket_permiteSoporte() {
        RevisionTicket ticket = RevisionTicket.builder().idTicket(1L).pedido(pedido).motivo(motivo).descripcionCliente("x").estadoTicket("Abierto").build();
        given(ticketRevisionRepository.findById(1L)).willReturn(Optional.of(ticket));
        given(ticketRevisionRepository.save(any(RevisionTicket.class))).willAnswer(inv -> inv.getArgument(0));
        autenticarComo("soporte@test.dev", "TICKET_RESOLVER");

        RevisionTicketResponse respuesta = ticketRevisionServicio.cambiarEstadoTicket(1L, 99L, "Resuelto");

        assertThat(respuesta.getEstadoTicket()).isEqualTo("Resuelto");
    }

    @Test
    @DisplayName("cambiarEstadoTicket permite a ADMIN aunque no sea el creador del servicio")
    void cambiarEstadoTicket_permiteAdmin() {
        RevisionTicket ticket = RevisionTicket.builder().idTicket(1L).pedido(pedido).motivo(motivo).descripcionCliente("x").estadoTicket("Abierto").build();
        given(ticketRevisionRepository.findById(1L)).willReturn(Optional.of(ticket));
        given(ticketRevisionRepository.save(any(RevisionTicket.class))).willAnswer(inv -> inv.getArgument(0));
        autenticarComo("admin@test.dev", "ROLE_ADMIN");

        RevisionTicketResponse respuesta = ticketRevisionServicio.cambiarEstadoTicket(1L, 99L, "Resuelto");

        assertThat(respuesta.getEstadoTicket()).isEqualTo("Resuelto");
    }

    @Test
    @DisplayName("cambiarEstadoTicket lanza recurso no encontrado si el ticket no existe")
    void cambiarEstadoTicket_inexistente() {
        given(ticketRevisionRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketRevisionServicio.cambiarEstadoTicket(1L, 2L, "Resuelto"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void autenticarComo(String correo, String... authorities) {
        List<SimpleGrantedAuthority> roles = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        var auth = new UsernamePasswordAuthenticationToken(correo, "N/A", roles);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
