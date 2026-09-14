package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.RevisionTicketPayment;

import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link RevisionTicketPayment}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL) mediante @Query
 * para resolver bloqueos pesimistas y accesos alternativos a la entidad.
 */
@Repository
public interface RevisionTicketPaymentRepository extends JpaRepository<RevisionTicketPayment, Long> {

    /** El pago asociado a un ticket de revisión (relación 1:1), si existe. */
    Optional<RevisionTicketPayment> findByTicketIdTicket(Long idTicket);

    /** El pago por el id de orden de PayPal, usado por el webhook. */
    Optional<RevisionTicketPayment> findByIdOrdenPaypal(String idOrdenPaypal);

    /** Con bloqueo pesimista: serializa la carrera entre el webhook de PayPal y RevisionTicketExpirationService. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM RevisionTicketPayment p WHERE p.ticket.idTicket = :idTicket")
    Optional<RevisionTicketPayment> findByTicketIdTicketParaActualizar(@Param("idTicket") Long idTicket);
}
