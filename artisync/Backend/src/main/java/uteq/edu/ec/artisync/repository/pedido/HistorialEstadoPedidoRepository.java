package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.HistorialEstadoPedido;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistorialEstadoPedidoRepository extends JpaRepository<HistorialEstadoPedido, Long> {

    List<HistorialEstadoPedido> findByPedidoIdPedidoOrderByFechaTransicionAsc(Long idPedido);

    Optional<HistorialEstadoPedido> findTopByPedidoIdPedidoOrderByFechaTransicionDesc(Long idPedido);
}
