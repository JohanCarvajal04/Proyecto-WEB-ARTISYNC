package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.TicketRevision;

import java.util.List;

@Repository
public interface TicketRevisionRepository extends JpaRepository<TicketRevision, Long> {

    List<TicketRevision> findByPedidoIdPedidoOrderByIdTicketDesc(Long idPedido);

    long countByPedidoIdPedido(Long idPedido);
}
