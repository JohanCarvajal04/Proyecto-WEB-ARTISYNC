package uteq.edu.ec.artisync.repository.pedido;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import uteq.edu.ec.artisync.entity.catalogo.Workflow;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.legal.RevisionTicketPayment;
import uteq.edu.ec.artisync.entity.pedido.RejectionReason;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.pedido.RevisionTicket;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.repository.catalogo.WorkflowRepository;
import uteq.edu.ec.artisync.repository.catalogo.OfferingRepository;
import uteq.edu.ec.artisync.repository.legal.RevisionTicketPaymentRepository;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * REQ-F-022c: findVencidosSinPagoConfirmado usa un LEFT JOIN con ON explícito
 * (RevisionTicketPayment no tiene una relación mapeada de vuelta a RevisionTicket)
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

    @Autowired private UserRepository usuarioRepository;
    @Autowired private CreatorProfileRepository perfilCreadorRepository;
    @Autowired private OfferingRepository servicioRepository;
    @Autowired private WorkflowRepository flujoTrabajoRepository;
    @Autowired private OrderRepository pedidoRepository;
    @Autowired private RejectionReasonRepository motivoRechazoRepository;
    @Autowired private RevisionTicketRepository ticketRevisionRepository;
    @Autowired private RevisionTicketPaymentRepository pagoTicketRevisionRepository;
    @Autowired private EntityManager entityManager;

    private Order pedido;
    private RejectionReason motivo;

    @BeforeEach
    void sembrarDatos() {
        User cliente = usuarioRepository.save(User.builder()
                .nombres("Cliente").apellidos("Prueba").correo("cliente-ticket-it@test.dev")
                .contrasenaHash("x").estadoCuenta(true).build());
        User creador = usuarioRepository.save(User.builder()
                .nombres("Creador").apellidos("Prueba").correo("creador-ticket-it@test.dev")
                .contrasenaHash("x").estadoCuenta(true).build());
        CreatorProfile perfil = perfilCreadorRepository.save(CreatorProfile.builder().usuario(creador).build());
        Offering servicio = servicioRepository.save(Offering.builder()
                .perfil(perfil).tituloServicio("Offering de prueba")
                .descripcionDetallada("Descripcion de prueba con longitud suficiente para pasar validacion")
                .precioBase(new BigDecimal("100.00")).build());
        Workflow flujo = flujoTrabajoRepository.save(Workflow.builder()
                .nombreFlujo("Flujo de prueba " + System.nanoTime()).creador(creador).build());
        pedido = pedidoRepository.save(Order.builder()
                .usuarioCliente(cliente).servicio(servicio).flujo(flujo)
                .precioPactado(new BigDecimal("100.00")).build());
        motivo = motivoRechazoRepository.save(RejectionReason.builder()
                .descripcionMotivo("Motivo de prueba " + System.nanoTime()).build());
    }

    private RevisionTicket crearTicket(String estado, BigDecimal costoAdicional, int horasDeAntiguedad) {
        RevisionTicket ticket = ticketRevisionRepository.saveAndFlush(RevisionTicket.builder()
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
        RevisionTicket vencidoSinPago = crearTicket("Abierto", new BigDecimal("5.00"), 51);

        RevisionTicket vencidoConPagoPendiente = crearTicket("Abierto", new BigDecimal("5.00"), 51);
        pagoTicketRevisionRepository.saveAndFlush(RevisionTicketPayment.builder()
                .ticket(vencidoConPagoPendiente).monto(new BigDecimal("5.00")).estadoPago("Pendiente").build());

        RevisionTicket vencidoYaPagado = crearTicket("Abierto", new BigDecimal("5.00"), 51);
        pagoTicketRevisionRepository.saveAndFlush(RevisionTicketPayment.builder()
                .ticket(vencidoYaPagado).monto(new BigDecimal("5.00")).estadoPago("Pagado").build());

        crearTicket("Abierto", new BigDecimal("5.00"), 0);   // reciente: NO debe salir
        crearTicket("Abierto", BigDecimal.ZERO, 51);          // sin cargo: NO debe salir
        crearTicket("Resuelto", new BigDecimal("5.00"), 51);  // ya resuelto: NO debe salir

        entityManager.clear();

        List<RevisionTicket> vencidos = ticketRevisionRepository
                .findVencidosSinPagoConfirmado(LocalDateTime.now().minusHours(48));

        assertThat(vencidos)
                .extracting(RevisionTicket::getIdTicket)
                .containsExactlyInAnyOrder(vencidoSinPago.getIdTicket(), vencidoConPagoPendiente.getIdTicket());
    }
}
