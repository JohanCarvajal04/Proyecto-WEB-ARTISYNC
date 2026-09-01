package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import uteq.edu.ec.artisync.entity.pedido.PropuestaTerminosPedido;

import java.util.Optional;

public interface PropuestaTerminosPedidoRepository extends JpaRepository<PropuestaTerminosPedido, Long> {

    Optional<PropuestaTerminosPedido> findByPedidoIdPedidoAndEstado(Long idPedido, String estado);
}
