package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.PagoTicketRevision;

import java.util.Optional;

@Repository
public interface PagoTicketRevisionRepository extends JpaRepository<PagoTicketRevision, Long> {

    Optional<PagoTicketRevision> findByTicketIdTicket(Long idTicket);

    Optional<PagoTicketRevision> findByIdOrdenPaypal(String idOrdenPaypal);

    /** Con bloqueo pesimista: serializa la carrera entre el webhook de PayPal y TicketRevisionExpiracionServicio. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PagoTicketRevision p WHERE p.ticket.idTicket = :idTicket")
    Optional<PagoTicketRevision> findByTicketIdTicketParaActualizar(@Param("idTicket") Long idTicket);
}
