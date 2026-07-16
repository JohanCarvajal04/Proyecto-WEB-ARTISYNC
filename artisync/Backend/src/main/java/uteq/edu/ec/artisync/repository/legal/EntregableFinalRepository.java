package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.EntregableFinal;

import java.util.Optional;

@Repository
public interface EntregableFinalRepository extends JpaRepository<EntregableFinal, Long> {

    Optional<EntregableFinal> findByPedidoIdPedido(Long idPedido);

    boolean existsByPedidoIdPedido(Long idPedido);
}
