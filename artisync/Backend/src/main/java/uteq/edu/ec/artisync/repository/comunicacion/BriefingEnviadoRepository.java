package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingEnviado;

import java.util.Optional;

@Repository
public interface BriefingEnviadoRepository extends JpaRepository<BriefingEnviado, Long> {

    Optional<BriefingEnviado> findByPedidoIdPedido(Long idPedido);

    boolean existsByPedidoIdPedido(Long idPedido);
}
