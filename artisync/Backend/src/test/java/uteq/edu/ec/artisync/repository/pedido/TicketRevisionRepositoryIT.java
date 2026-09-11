package uteq.edu.ec.artisync.repository.pedido;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.PagoTicketRevision;
import uteq.edu.ec.artisync.entity.pedido.MotivoRechazo;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.TicketRevision;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.catalogo.ServicioRepository;
import uteq.edu.ec.artisync.repository.legal.PagoTicketRevisionRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REQ-F-022c: findVencidosSinPagoConfirmado usa un LEFT JOIN con ON explícito
 * (PagoTicketRevision no tiene una relación mapeada de vuelta a TicketRevision)
 * que Mockito no valida — necesita ejecutar de verdad contra un motor JPA. La
 * query es JPQL puro (sin procedimiento almacenado), pero corre contra
 * Postgres real como el resto de *IT: sin Replace.NONE, @DataJpaTest sustituye
 * el datasource por un H2 embebido aunque el perfil postgres-it este forzado
 * externamente (CI: -Dtest='*IT' -Dspring.profiles.active=postgres-it), y
 * Flyway migra Postgres mientras Hibernate valida contra el H2 vacio.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("postgres-it")
class TicketRevisionRepositoryIT {

    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PerfilCreadorRepository perfilCreadorRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private FlujoTrabajoRepository flujoTrabajoRepository;
    @Autowired private PedidoRepository pedidoRepository;
    @Autowired private MotivoRechazoRepository motivoRechazoRepository;
    @Autowired private TicketRevisionRepository ticketRevisionRepository;
    @Autowired private PagoTicketRevisionRepository pagoTicketRevisionRepository;
    @Autowired private EntityManager entityManager;

    private Pedido pedido;
    private MotivoRechazo motivo;

    @BeforeEach
    void sembrarDatos() {
        Usuario cliente = usuarioRepository.save(Usuario.builder()
                .nombres("Cliente").apellidos("Prueba").correo("cliente-ticket-it@test.dev")
                .contrasenaHash("x").estadoCuenta(true).build());
        Usuario creador = usuarioRepository.save(Usuario.builder()
                .nombres("Creador").apellidos("Prueba").correo("creador-ticket-it@test.dev")
                .contrasenaHash("x").estadoCuenta(true).build());
        PerfilCreador perfil = perfilCreadorRepository.save(PerfilCreador.builder().usuario(creador).build());
        Servicio servicio = servicioRepository.save(Servicio.builder()
                .perfil(perfil).tituloServicio("Servicio de prueba")
                .descripcionDetallada("Descripcion de prueba con longitud suficiente para pasar validacion")
                .precioBase(new BigDecimal("100.00")).build());
        FlujoTrabajo flujo = flujoTrabajoRepository.save(FlujoTrabajo.builder()
                .nombreFlujo("Flujo de prueba " + System.nanoTime()).creador(creador).build());
        pedido = pedidoRepository.save(Pedido.builder()
                .usuarioCliente(cliente).servicio(servicio).flujo(flujo)
                .precioPactado(new BigDecimal("100.00")).build());
        motivo = motivoRechazoRepository.save(MotivoRechazo.builder()
                .descripcionMotivo("Motivo de prueba " + System.nanoTime()).build());
    }

    private TicketRevision crearTicket(String estado, BigDecimal costoAdicional, int horasDeAntiguedad) {
        TicketRevision ticket = ticketRevisionRepository.saveAndFlush(TicketRevision.builder()
                .pedido(pedido).motivo(motivo).descripcionCliente("desc")
                .estadoTicket(estado).costoAdicionalGenerado(costoAdicional).build());
        // @CreationTimestamp fuerza fecha_creacion=now() al insertar; se
        // retrasa con una actualizacion nativa para simular un ticket viejo.
        if (horasDeAntiguedad > 0) {
            entityManager.createNativeQuery("UPDATE tickets_revision SET fecha_creacion = ?1 WHERE id_ticket = ?2")
                    .setParameter(1, LocalDateTime.now().minusHours(horasDeAntiguedad))
                    .setParameter(2, ticket.getIdTicket())
                    .executeUpdate();
        }
        return ticket;
    }

    @Test
    @DisplayName("solo devuelve tickets Abiertos, con cargo generado, vencidos y sin pago confirmado")
    void findVencidosSinPagoConfirmado_filtraCorrectamente() {
        TicketRevision vencidoSinPago = crearTicket("Abierto", new BigDecimal("5.00"), 51);

        TicketRevision vencidoConPagoPendiente = crearTicket("Abierto", new BigDecimal("5.00"), 51);
        pagoTicketRevisionRepository.saveAndFlush(PagoTicketRevision.builder()
                .ticket(vencidoConPagoPendiente).monto(new BigDecimal("5.00")).estadoPago("Pendiente").build());

        TicketRevision vencidoYaPagado = crearTicket("Abierto", new BigDecimal("5.00"), 51);
        pagoTicketRevisionRepository.saveAndFlush(PagoTicketRevision.builder()
                .ticket(vencidoYaPagado).monto(new BigDecimal("5.00")).estadoPago("Pagado").build());

        crearTicket("Abierto", new BigDecimal("5.00"), 0);   // reciente: NO debe salir
        crearTicket("Abierto", BigDecimal.ZERO, 51);          // sin cargo: NO debe salir
        crearTicket("Resuelto", new BigDecimal("5.00"), 51);  // ya resuelto: NO debe salir

        entityManager.clear();

        List<TicketRevision> vencidos = ticketRevisionRepository
                .findVencidosSinPagoConfirmado(LocalDateTime.now().minusHours(48));

        assertThat(vencidos)
                .extracting(TicketRevision::getIdTicket)
                .containsExactlyInAnyOrder(vencidoSinPago.getIdTicket(), vencidoConPagoPendiente.getIdTicket());
    }
}
