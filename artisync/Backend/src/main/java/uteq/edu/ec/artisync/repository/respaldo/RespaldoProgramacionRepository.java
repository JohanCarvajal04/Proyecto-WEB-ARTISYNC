package uteq.edu.ec.artisync.repository.respaldo;

import org.springframework.data.jpa.repository.JpaRepository;
import uteq.edu.ec.artisync.entity.respaldo.RespaldoProgramacion;

import java.time.LocalDateTime;
import java.util.List;

public interface RespaldoProgramacionRepository extends JpaRepository<RespaldoProgramacion, Long> {

    List<RespaldoProgramacion> findByActivoTrueAndProximaEjecucionLessThanEqual(LocalDateTime ahora);
}
