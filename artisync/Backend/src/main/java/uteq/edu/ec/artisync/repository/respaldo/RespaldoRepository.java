package uteq.edu.ec.artisync.repository.respaldo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import uteq.edu.ec.artisync.entity.respaldo.EstadoRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.Respaldo;

import java.util.List;
import java.util.Optional;

public interface RespaldoRepository extends JpaRepository<Respaldo, Long>, JpaSpecificationExecutor<Respaldo> {

    boolean existsByEstadoRespaldo(EstadoRespaldo estado);

    List<Respaldo> findByEstadoRespaldo(EstadoRespaldo estado);

    Optional<Respaldo> findTopByIdRespaldoFullBaseOrderByFechaInicioDesc(Long idRespaldoFullBase);

    boolean existsByIdRespaldoFullBaseAndEstadoRespaldo(Long idRespaldoFullBase, EstadoRespaldo estado);

    Optional<Respaldo> findTopByTipoRespaldoAndEstadoRespaldoOrderByFechaInicioDesc(
            uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo tipo, EstadoRespaldo estado);
}
