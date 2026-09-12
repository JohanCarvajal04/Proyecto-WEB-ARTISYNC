package uteq.edu.ec.artisync.repository.pedido;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.RevisionTicket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link RevisionTicket}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface RevisionTicketRepository extends JpaRepository<RevisionTicket, Long> {

    /** Tickets de revisión de un pedido, más recientes primero. */
    List<RevisionTicket> findByPedidoIdPedidoOrderByIdTicketDesc(Long idPedido);

    /** Cantidad de tickets de revisión de un pedido. */
    long countByPedidoIdPedido(Long idPedido);

    /**
     * REQ-F-022c: tickets 'Abierto' que SÍ generaron cargo y siguen sin pago
     * confirmado más allá del umbral. Un ticket que nunca superó el límite no
     * tiene nada que pagar y no debe auto-rechazarse (costoAdicionalGenerado = 0
     * los excluye). LEFT JOIN con ON explícito: RevisionTicketPayment no tiene una
     * relación mapeada de vuelta a RevisionTicket.
     */
    @Query("SELECT t FROM RevisionTicket t LEFT JOIN RevisionTicketPayment p ON p.ticket = t " +
            "WHERE t.estadoTicket = 'Abierto' AND t.costoAdicionalGenerado > 0 " +
            "AND t.fechaCreacion < :limite AND (p IS NULL OR p.estadoPago <> 'Pagado')")
    List<RevisionTicket> findVencidosSinPagoConfirmado(@Param("limite") LocalDateTime limite);

    /** Con bloqueo pesimista: serializa la carrera entre RevisionTicketExpirationService y el webhook/creador. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RevisionTicket t WHERE t.idTicket = :idTicket")
    Optional<RevisionTicket> findByIdParaActualizar(@Param("idTicket") Long idTicket);
}
