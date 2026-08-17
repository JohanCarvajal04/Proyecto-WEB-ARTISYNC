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
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearTicketRevision;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaTicketRevision;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.pedido.MotivoRechazo;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.TicketRevision;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.pedido.MotivoRechazoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.pedido.TicketRevisionRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TicketRevisionServicioImplTest {

    @Mock private TicketRevisionRepository ticketRevisionRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private MotivoRechazoRepository motivoRechazoRepository;
    @Mock private ContratoRepository contratoRepository;

    @InjectMocks
    private TicketRevisionServicioImpl ticketRevisionServicio;

    private Usuario cliente;
    private Usuario creador;
    private Pedido pedido;
    private MotivoRechazo motivo;

    @BeforeEach
    void setUp() {
        cliente = Usuario.builder().idUsuario(1L).nombres("Cliente").apellidos("Uno").correo("cliente@test.com").build();
        creador = Usuario.builder().idUsuario(2L).nombres("Creador").apellidos("Uno").correo("creador@test.com").build();
        PerfilCreador perfil = PerfilCreador.builder().idPerfil(1L).usuario(creador).build();
        Servicio servicio = Servicio.builder().idServicio(1L).perfil(perfil).cargoRevisionAdicional(new BigDecimal("5.00")).build();
        pedido = Pedido.builder().idPedido(1L).usuarioCliente(cliente).servicio(servicio).precioPactado(BigDecimal.TEN).build();
        motivo = MotivoRechazo.builder().idMotivo(1L).descripcionMotivo("Calidad insuficiente").build();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("crearTicketRevision guarda el ticket cuando lo crea el cliente del pedido")
    void crearTicketRevision_guarda() {
        PeticionCrearTicketRevision peticion = PeticionCrearTicketRevision.builder().idMotivo(1L).descripcionCliente("No cumple").build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(motivoRechazoRepository.findById(1L)).willReturn(Optional.of(motivo));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.empty());
        given(ticketRevisionRepository.save(any(TicketRevision.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaTicketRevision respuesta = ticketRevisionServicio.crearTicketRevision(1L, 1L, peticion);

        assertThat(respuesta.getDescripcionCliente()).isEqualTo("No cumple");
        assertThat(respuesta.getEstadoTicket()).isEqualTo("Abierto");
    }

    @Test
    @DisplayName("crearTicketRevision rechaza a un usuario que no es el cliente del pedido")
    void crearTicketRevision_rechazaNoCliente() {
        PeticionCrearTicketRevision peticion = PeticionCrearTicketRevision.builder().idMotivo(1L).descripcionCliente("No cumple").build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> ticketRevisionServicio.crearTicketRevision(1L, 99L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("crearTicketRevision lanza recurso no encontrado si el pedido no existe")
    void crearTicketRevision_pedidoInexistente() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketRevisionServicio.crearTicketRevision(1L, 1L, PeticionCrearTicketRevision.builder().build()))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("crearTicketRevision lanza recurso no encontrado si el motivo no existe")
    void crearTicketRevision_motivoInexistente() {
        PeticionCrearTicketRevision peticion = PeticionCrearTicketRevision.builder().idMotivo(99L).descripcionCliente("No cumple").build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(motivoRechazoRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketRevisionServicio.crearTicketRevision(1L, 1L, peticion))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("crearTicketRevision marca cargo adicional cuando supera el limite de revisiones del contrato")
    void crearTicketRevision_marcaCargoAdicional() {
        PeticionCrearTicketRevision peticion = PeticionCrearTicketRevision.builder().idMotivo(1L).descripcionCliente("No cumple").build();
        Contrato contrato = Contrato.builder().limiteRevisiones(1).build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(motivoRechazoRepository.findById(1L)).willReturn(Optional.of(motivo));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contrato));
        given(ticketRevisionRepository.countByPedidoIdPedido(1L)).willReturn(1L);
        given(ticketRevisionRepository.save(any(TicketRevision.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaTicketRevision respuesta = ticketRevisionServicio.crearTicketRevision(1L, 1L, peticion);

        assertThat(respuesta.getCostoAdicionalGenerado()).isEqualByComparingTo("5.00");
    }

    @Test
    @DisplayName("crearTicketRevision no marca cargo adicional si aun no se alcanza el limite")
    void crearTicketRevision_sinCargoAdicionalDentroDelLimite() {
        PeticionCrearTicketRevision peticion = PeticionCrearTicketRevision.builder().idMotivo(1L).descripcionCliente("No cumple").build();
        Contrato contrato = Contrato.builder().limiteRevisiones(3).build();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(motivoRechazoRepository.findById(1L)).willReturn(Optional.of(motivo));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contrato));
        given(ticketRevisionRepository.countByPedidoIdPedido(1L)).willReturn(1L);
        given(ticketRevisionRepository.save(any(TicketRevision.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaTicketRevision respuesta = ticketRevisionServicio.crearTicketRevision(1L, 1L, peticion);

        assertThat(respuesta.getCostoAdicionalGenerado()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("listarTicketsPorPedido devuelve los tickets cuando el solicitante es el cliente")
    void listarTicketsPorPedido_clientePuedeVer() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(ticketRevisionRepository.findByPedidoIdPedidoOrderByIdTicketDesc(1L))
                .willReturn(List.of(TicketRevision.builder().idTicket(1L).pedido(pedido).motivo(motivo).descripcionCliente("x").build()));

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
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("cambiarEstadoTicket actualiza el estado cuando lo hace el creador del servicio")
    void cambiarEstadoTicket_actualiza() {
        TicketRevision ticket = TicketRevision.builder().idTicket(1L).pedido(pedido).motivo(motivo).descripcionCliente("x").estadoTicket("Abierto").build();
        given(ticketRevisionRepository.findById(1L)).willReturn(Optional.of(ticket));
        given(ticketRevisionRepository.save(any(TicketRevision.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaTicketRevision respuesta = ticketRevisionServicio.cambiarEstadoTicket(1L, 2L, "Resuelto");

        assertThat(respuesta.getEstadoTicket()).isEqualTo("Resuelto");
    }

    @Test
    @DisplayName("cambiarEstadoTicket rechaza a un usuario que no es el creador del servicio")
    void cambiarEstadoTicket_rechazaNoCreador() {
        TicketRevision ticket = TicketRevision.builder().idTicket(1L).pedido(pedido).motivo(motivo).descripcionCliente("x").build();
        given(ticketRevisionRepository.findById(1L)).willReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketRevisionServicio.cambiarEstadoTicket(1L, 99L, "Resuelto"))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("cambiarEstadoTicket lanza recurso no encontrado si el ticket no existe")
    void cambiarEstadoTicket_inexistente() {
        given(ticketRevisionRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketRevisionServicio.cambiarEstadoTicket(1L, 2L, "Resuelto"))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    private void autenticarComo(String correo, String... authorities) {
        List<SimpleGrantedAuthority> roles = List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList();
        var auth = new UsernamePasswordAuthenticationToken(correo, "N/A", roles);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
