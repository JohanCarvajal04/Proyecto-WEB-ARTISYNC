package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.TransaccionPago;

import java.util.List;

@Repository
public interface TransaccionPagoRepository extends JpaRepository<TransaccionPago, Long> {

    List<TransaccionPago> findByPagoIdPagoOrderByFechaEjecucionDesc(Long idPago);
}
